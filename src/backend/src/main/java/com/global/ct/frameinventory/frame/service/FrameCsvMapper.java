package com.global.ct.frameinventory.frame.service;

import com.global.ct.frameinventory.frame.dto.CreateFrameRequest;
import com.global.ct.frameinventory.frame.exception.InvalidCsvRowException;
import com.global.ct.frameinventory.frame.model.FrameEnvironment;
import com.global.ct.frameinventory.frame.model.FrameStatus;
import com.global.ct.frameinventory.frame.model.MediaType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Component
class FrameCsvMapper {

    static final Set<String> REQUIRED_HEADERS = Set.of(
        "frame_id", "type_classic_digital", "format", "environment", "site_no", "station",
        "address", "region", "country_code", "town", "postcode", "longitude", "latitude",
        "status", "status_reason", "number_of_slots", "distance_to_closest_school",
        "pixel_height", "pixel_width", "premium"
    );

    private final Validator validator;

    FrameCsvMapper(Validator validator) {
        this.validator = validator;
    }

    FrameImportRow map(CSVRecord record) {
        CreateFrameRequest request = new CreateFrameRequest(
            required(record, "frame_id"),
            enumValue(record, "type_classic_digital", MediaType.class),
            required(record, "format"),
            enumValue(record, "environment", FrameEnvironment.class),
            optional(record, "site_no"),
            optional(record, "station"),
            optional(record, "address"),
            optional(record, "region"),
            required(record, "country_code"),
            optional(record, "town"),
            optional(record, "postcode"),
            decimal(record, "longitude"),
            decimal(record, "latitude"),
            enumValue(record, "status", FrameStatus.class),
            optional(record, "status_reason"),
            integer(record, "number_of_slots"),
            integer(record, "distance_to_closest_school"),
            integer(record, "pixel_height"),
            integer(record, "pixel_width"),
            booleanValue(record, "premium")
        );

        Set<ConstraintViolation<CreateFrameRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
            throw new InvalidCsvRowException(detail);
        }
        return new FrameImportRow(request.frameId(), request.toData());
    }

    private String required(CSVRecord record, String header) {
        String value = value(record, header);
        if (value.isEmpty()) {
            throw new InvalidCsvRowException(header + " is required");
        }
        return value;
    }

    private String optional(CSVRecord record, String header) {
        String value = value(record, header);
        return value.isEmpty() ? null : value;
    }

    private BigDecimal decimal(CSVRecord record, String header) {
        String value = value(record, header);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new InvalidCsvRowException(header + " must be a decimal number");
        }
    }

    private Integer integer(CSVRecord record, String header) {
        String value = value(record, header);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new InvalidCsvRowException(header + " must be an integer");
        }
    }

    private Boolean booleanValue(CSVRecord record, String header) {
        return switch (required(record, header).toLowerCase(Locale.ROOT)) {
            case "1", "true" -> true;
            case "0", "false" -> false;
            default -> throw new InvalidCsvRowException(header + " must be 0, 1, true, or false");
        };
    }

    private <E extends Enum<E>> E enumValue(CSVRecord record, String header, Class<E> type) {
        String value = required(record, header);
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidCsvRowException(header + " has unsupported value '" + value + "'");
        }
    }

    private String value(CSVRecord record, String header) {
        return record.get(header).trim();
    }
}
