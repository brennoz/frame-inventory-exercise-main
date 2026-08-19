package com.global.ct.frameinventory.frame.service;

import com.global.ct.frameinventory.frame.dto.FrameImportErrorResponse;
import com.global.ct.frameinventory.frame.dto.FrameImportResponse;
import com.global.ct.frameinventory.frame.exception.InvalidCsvException;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FrameImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrameImportService.class);
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
        ImportCounts counts = new ImportCounts();
        List<FrameImportErrorResponse> errors = new ArrayList<>();

        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSV_FORMAT.parse(reader)) {
            validateHeaders(parser);
            for (CSVRecord record : parser) {
                importRecord(record, counts, errors);
            }
        } catch (IOException | UncheckedIOException exception) {
            throw new InvalidCsvException("The CSV file could not be read", exception);
        } catch (IllegalArgumentException exception) {
            throw new InvalidCsvException("The CSV structure is invalid", exception);
        }

        return new FrameImportResponse(counts.created, counts.updated, counts.unchanged, counts.failed, errors);
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

    private void importRecord(CSVRecord record, ImportCounts counts, List<FrameImportErrorResponse> errors) {
        long rowNumber = record.getRecordNumber() + 1;
        String frameId = frameId(record);
        try {
            if (!record.isConsistent()) {
                throw new IllegalArgumentException("row has a different number of columns than the header");
            }
            FrameImportRow row = mapper.map(record);
            FrameImportOutcome outcome = commandService.upsertImportedFrame(row.frameId(), row.data());
            counts.record(outcome);
        } catch (RuntimeException exception) {
            counts.failed++;
            if (!(exception instanceof IllegalArgumentException)) {
                LOGGER.warn("CSV row could not be persisted rowNumber={} frameId={}",
                    rowNumber, frameId, exception);
            }
            errors.add(new FrameImportErrorResponse(rowNumber, frameId, safeMessage(exception)));
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
        if (exception instanceof IllegalArgumentException && exception.getMessage() != null) {
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
