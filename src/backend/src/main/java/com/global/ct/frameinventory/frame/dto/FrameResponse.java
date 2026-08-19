package com.global.ct.frameinventory.frame.dto;

import com.global.ct.frameinventory.frame.model.Frame;
import com.global.ct.frameinventory.frame.model.FrameEnvironment;
import com.global.ct.frameinventory.frame.model.FrameStatus;
import com.global.ct.frameinventory.frame.model.MediaType;
import java.math.BigDecimal;
import java.time.Instant;

public record FrameResponse(
    String frameId,
    MediaType mediaType,
    String format,
    FrameEnvironment environment,
    String siteNumber,
    String station,
    String address,
    String region,
    String countryCode,
    String town,
    String postcode,
    BigDecimal longitude,
    BigDecimal latitude,
    FrameStatus status,
    String statusReason,
    Integer numberOfSlots,
    Integer distanceToClosestSchool,
    Integer pixelHeight,
    Integer pixelWidth,
    boolean premium,
    Instant createdAt,
    Instant updatedAt,
    long version
) {
    public static FrameResponse from(Frame frame) {
        return new FrameResponse(
            frame.getFrameId(), frame.getMediaType(), frame.getFormat(), frame.getEnvironment(),
            frame.getSiteNumber(), frame.getStation(), frame.getAddress(), frame.getRegion(),
            frame.getCountryCode(), frame.getTown(), frame.getPostcode(), frame.getLongitude(),
            frame.getLatitude(), frame.getStatus(), frame.getStatusReason(), frame.getNumberOfSlots(),
            frame.getDistanceToClosestSchool(), frame.getPixelHeight(), frame.getPixelWidth(),
            frame.isPremium(), frame.getCreatedAt(), frame.getUpdatedAt(), frame.getVersion()
        );
    }
}
