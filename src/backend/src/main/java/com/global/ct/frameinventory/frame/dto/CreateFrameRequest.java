package com.global.ct.frameinventory.frame.dto;

import com.global.ct.frameinventory.frame.model.FrameData;
import com.global.ct.frameinventory.frame.model.FrameEnvironment;
import com.global.ct.frameinventory.frame.model.FrameStatus;
import com.global.ct.frameinventory.frame.model.MediaType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateFrameRequest(
    @NotBlank @Size(max = 64)
    @Pattern(regexp = "[A-Za-z0-9_-]+", message = "must contain only letters, numbers, underscores, or hyphens")
    String frameId,
    @NotNull MediaType mediaType,
    @NotBlank @Size(max = 100) String format,
    @NotNull FrameEnvironment environment,
    @Size(max = 64) String siteNumber,
    @Size(max = 150) String station,
    @Size(max = 500) String address,
    @Size(max = 100) String region,
    @NotBlank @Size(max = 8) String countryCode,
    @Size(max = 150) String town,
    @Size(max = 16) String postcode,
    @DecimalMin("-180.0") @DecimalMax("180.0") @Digits(integer = 3, fraction = 8) BigDecimal longitude,
    @DecimalMin("-90.0") @DecimalMax("90.0") @Digits(integer = 2, fraction = 8) BigDecimal latitude,
    @NotNull FrameStatus status,
    @Size(max = 255) String statusReason,
    @PositiveOrZero Integer numberOfSlots,
    @PositiveOrZero Integer distanceToClosestSchool,
    @PositiveOrZero Integer pixelHeight,
    @PositiveOrZero Integer pixelWidth,
    @NotNull Boolean premium
) {
    public FrameData toData() {
        return new FrameData(
            mediaType, format, environment, siteNumber, station, address, region, countryCode,
            town, postcode, longitude, latitude, status, statusReason, numberOfSlots,
            distanceToClosestSchool, pixelHeight, pixelWidth, premium
        );
    }
}
