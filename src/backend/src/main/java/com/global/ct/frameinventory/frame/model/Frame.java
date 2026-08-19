package com.global.ct.frameinventory.frame.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "frames")
public class Frame {

    @Id
    @Column(name = "frame_id", length = 64, nullable = false, updatable = false)
    private String frameId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", length = 16, nullable = false)
    private MediaType mediaType;

    @Column(name = "format", length = 100, nullable = false)
    private String format;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", length = 32, nullable = false)
    private FrameEnvironment environment;

    @Column(name = "site_number", length = 64)
    private String siteNumber;

    @Column(name = "station", length = 150)
    private String station;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "country_code", length = 8, nullable = false)
    private String countryCode;

    @Column(name = "town", length = 150)
    private String town;

    @Column(name = "postcode", length = 16)
    private String postcode;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private FrameStatus status;

    @Column(name = "status_reason", length = 255)
    private String statusReason;

    @Column(name = "number_of_slots")
    private Integer numberOfSlots;

    @Column(name = "distance_to_closest_school")
    private Integer distanceToClosestSchool;

    @Column(name = "pixel_height")
    private Integer pixelHeight;

    @Column(name = "pixel_width")
    private Integer pixelWidth;

    @Column(name = "premium", nullable = false)
    private boolean premium;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Frame() {
    }

    public static Frame create(String frameId, FrameData data, Instant now) {
        Frame frame = new Frame();
        frame.frameId = frameId;
        frame.apply(data);
        frame.createdAt = now;
        frame.updatedAt = now;
        return frame;
    }

    public List<FrameFieldChange> update(FrameData data, Instant now) {
        List<FrameFieldChange> changes = data.changesFrom(data());
        if (!changes.isEmpty()) {
            apply(data);
            updatedAt = now;
        }
        return changes;
    }

    public FrameData data() {
        return new FrameData(
            mediaType, format, environment, siteNumber, station, address, region, countryCode,
            town, postcode, longitude, latitude, status, statusReason, numberOfSlots,
            distanceToClosestSchool, pixelHeight, pixelWidth, premium
        );
    }

    private void apply(FrameData data) {
        mediaType = data.mediaType();
        format = data.format();
        environment = data.environment();
        siteNumber = data.siteNumber();
        station = data.station();
        address = data.address();
        region = data.region();
        countryCode = data.countryCode();
        town = data.town();
        postcode = data.postcode();
        longitude = data.longitude();
        latitude = data.latitude();
        status = data.status();
        statusReason = data.statusReason();
        numberOfSlots = data.numberOfSlots();
        distanceToClosestSchool = data.distanceToClosestSchool();
        pixelHeight = data.pixelHeight();
        pixelWidth = data.pixelWidth();
        premium = data.premium();
    }

    public String getFrameId() { return frameId; }
    public MediaType getMediaType() { return mediaType; }
    public String getFormat() { return format; }
    public FrameEnvironment getEnvironment() { return environment; }
    public String getSiteNumber() { return siteNumber; }
    public String getStation() { return station; }
    public String getAddress() { return address; }
    public String getRegion() { return region; }
    public String getCountryCode() { return countryCode; }
    public String getTown() { return town; }
    public String getPostcode() { return postcode; }
    public BigDecimal getLongitude() { return longitude; }
    public BigDecimal getLatitude() { return latitude; }
    public FrameStatus getStatus() { return status; }
    public String getStatusReason() { return statusReason; }
    public Integer getNumberOfSlots() { return numberOfSlots; }
    public Integer getDistanceToClosestSchool() { return distanceToClosestSchool; }
    public Integer getPixelHeight() { return pixelHeight; }
    public Integer getPixelWidth() { return pixelWidth; }
    public boolean isPremium() { return premium; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
