package com.global.ct.frameinventory.frame.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public record FrameData(
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
    Boolean premium
) {
    public List<FrameFieldChange> changesFrom(FrameData previous) {
        List<FrameFieldChange> changes = new ArrayList<>();
        compare(changes, "mediaType", previous, FrameData::mediaType);
        compare(changes, "format", previous, FrameData::format);
        compare(changes, "environment", previous, FrameData::environment);
        compare(changes, "siteNumber", previous, FrameData::siteNumber);
        compare(changes, "station", previous, FrameData::station);
        compare(changes, "address", previous, FrameData::address);
        compare(changes, "region", previous, FrameData::region);
        compare(changes, "countryCode", previous, FrameData::countryCode);
        compare(changes, "town", previous, FrameData::town);
        compare(changes, "postcode", previous, FrameData::postcode);
        compare(changes, "longitude", previous, FrameData::longitude);
        compare(changes, "latitude", previous, FrameData::latitude);
        compare(changes, "status", previous, FrameData::status);
        compare(changes, "statusReason", previous, FrameData::statusReason);
        compare(changes, "numberOfSlots", previous, FrameData::numberOfSlots);
        compare(changes, "distanceToClosestSchool", previous, FrameData::distanceToClosestSchool);
        compare(changes, "pixelHeight", previous, FrameData::pixelHeight);
        compare(changes, "pixelWidth", previous, FrameData::pixelWidth);
        compare(changes, "premium", previous, FrameData::premium);
        return changes;
    }

    private <T> void compare(List<FrameFieldChange> changes, String fieldName, FrameData previous,
                             Function<FrameData, T> getter) {
        T oldValue = previous == null ? null : getter.apply(previous);
        T newValue = getter.apply(this);
        if (!equivalent(oldValue, newValue)) {
            changes.add(new FrameFieldChange(fieldName, display(oldValue), display(newValue)));
        }
    }

    private boolean equivalent(Object oldValue, Object newValue) {
        if (oldValue instanceof BigDecimal oldDecimal && newValue instanceof BigDecimal newDecimal) {
            return oldDecimal.compareTo(newDecimal) == 0;
        }
        return Objects.equals(oldValue, newValue);
    }

    private String display(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return value == null ? null : value.toString();
    }
}
