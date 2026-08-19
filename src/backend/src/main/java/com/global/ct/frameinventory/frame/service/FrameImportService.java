package com.global.ct.frameinventory.frame.service;

import com.global.ct.frameinventory.frame.dto.FrameImportErrorResponse;
import com.global.ct.frameinventory.frame.dto.FrameImportResponse;
import com.global.ct.frameinventory.frame.exception.InvalidCsvException;
import com.global.ct.frameinventory.frame.exception.InvalidCsvRowException;
import com.global.ct.frameinventory.frame.model.FrameImportOutcome;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FrameImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrameImportService.class);
    private static final int MAX_IMPORT_ROWS = 5_000;
    private static final int MAX_REPORTED_ERRORS = 100;
    private static final CSVFormat CSV_FORMAT = CSVFormat.RFC4180.builder()
        .setHeader()
        .setSkipHeaderRecord(true)
        .setIgnoreEmptyLines(true)
        .setIgnoreSurroundingSpaces(true)
        .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW)
        .get();

    private final FrameCsvMapper mapper;
    private final FrameCommandService commandService;

    public FrameImportService(FrameCsvMapper mapper, FrameCommandService commandService) {
        this.mapper = mapper;
        this.commandService = commandService;
    }

    public FrameImportResponse importCsv(MultipartFile file) {
        validateFile(file);
        validateStructureAndRowCount(file);
        ImportCounts counts = new ImportCounts();
        List<FrameImportErrorResponse> errors = new ArrayList<>();

        try (CSVParser parser = openParser(file)) {
            for (CSVRecord record : parser) {
                importRecord(record, counts, errors);
            }
        } catch (IOException | UncheckedIOException exception) {
            throw new InvalidCsvException("The CSV file could not be read", exception);
        }

        boolean errorsTruncated = counts.failed > errors.size();
        LOGGER.info("CSV import completed created={} updated={} unchanged={} failed={} errorsTruncated={}",
            counts.created, counts.updated, counts.unchanged, counts.failed, errorsTruncated);
        return new FrameImportResponse(
            counts.created, counts.updated, counts.unchanged, counts.failed, errorsTruncated, List.copyOf(errors)
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidCsvException("A non-empty CSV file is required");
        }
        String filename = file.getOriginalFilename();
        if (filename != null && !filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new InvalidCsvException("The uploaded file must have a .csv extension");
        }
    }

    private void validateHeaders(CSVParser parser) {
        Set<String> missingHeaders = new TreeSet<>(FrameCsvMapper.REQUIRED_HEADERS);
        missingHeaders.removeAll(parser.getHeaderMap().keySet());
        if (!missingHeaders.isEmpty()) {
            throw new InvalidCsvException("Missing required headers: " + String.join(", ", missingHeaders));
        }
    }

    private void validateStructureAndRowCount(MultipartFile file) {
        try (CSVParser parser = openParser(file)) {
            validateHeaders(parser);
            int rowCount = 0;
            for (CSVRecord record : parser) {
                rowCount++;
                if (rowCount > MAX_IMPORT_ROWS) {
                    throw new InvalidCsvException("A CSV file may contain at most 5,000 data rows");
                }
                if (!record.isConsistent()) {
                    throw new InvalidCsvException(
                        "CSV row " + (record.getRecordNumber() + 1) + " has an unexpected number of columns"
                    );
                }
            }
        } catch (IOException | UncheckedIOException exception) {
            throw new InvalidCsvException("The CSV file could not be read", exception);
        } catch (IllegalArgumentException exception) {
            throw new InvalidCsvException("The CSV structure is invalid", exception);
        }
    }

    private CSVParser openParser(MultipartFile file) {
        Reader reader = null;
        try {
            reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
            );
            return CSV_FORMAT.parse(reader);
        } catch (IOException exception) {
            closeQuietly(reader);
            throw new InvalidCsvException("The CSV file could not be read", exception);
        } catch (IllegalArgumentException exception) {
            closeQuietly(reader);
            throw new InvalidCsvException("The CSV structure is invalid", exception);
        }
    }

    private void closeQuietly(Reader reader) {
        if (reader == null) {
            return;
        }
        try {
            reader.close();
        } catch (IOException ignored) {
            // The original parser failure is the useful error for the client.
        }
    }

    private void importRecord(CSVRecord record, ImportCounts counts, List<FrameImportErrorResponse> errors) {
        long rowNumber = record.getRecordNumber() + 1;
        String frameId = frameId(record);
        try {
            FrameImportRow row = mapper.map(record);
            FrameImportOutcome outcome = commandService.upsertImportedFrame(row.frameId(), row.data());
            counts.record(outcome);
        } catch (InvalidCsvRowException | DataIntegrityViolationException |
                 ObjectOptimisticLockingFailureException exception) {
            counts.failed++;
            if (errors.size() < MAX_REPORTED_ERRORS) {
                errors.add(new FrameImportErrorResponse(rowNumber, frameId, safeMessage(exception)));
            }
        }
    }

    private String frameId(CSVRecord record) {
        if (!record.isMapped("frame_id") || !record.isSet("frame_id")) {
            return null;
        }
        String frameId = record.get("frame_id").trim();
        return frameId.isEmpty() ? null : frameId;
    }

    private String safeMessage(RuntimeException exception) {
        if (exception instanceof InvalidCsvRowException && exception.getMessage() != null) {
            return exception.getMessage();
        }
        return "row could not be persisted";
    }

    private static final class ImportCounts {
        private int created;
        private int updated;
        private int unchanged;
        private int failed;

        private void record(FrameImportOutcome outcome) {
            switch (outcome) {
                case CREATED -> created++;
                case UPDATED -> updated++;
                case UNCHANGED -> unchanged++;
            }
        }
    }
}
