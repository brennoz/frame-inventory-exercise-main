package com.global.ct.frameinventory.frame.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.global.ct.frameinventory.DatabaseIntegrationTest;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FrameControllerIntegrationTest extends DatabaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    @Qualifier("applicationTaskExecutor")
    private AsyncTaskExecutor applicationTaskExecutor;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("delete from frame_revision_changes").update();
        jdbcClient.sql("delete from frame_revisions").update();
        jdbcClient.sql("delete from frames").update();

        insertFrame("FRAME-003", "DIGITAL", "D6", "RAIL", "Paddington", "London", "LIVE", "2026-08-18 10:00:00");
        insertFrame("FRAME-002", "CLASSIC", "48_SHEET", "ROADSIDE", null, "Bristol", "MAINTENANCE", "2026-08-19 09:00:00");
        insertFrame("FRAME-001", "DIGITAL", "D6", "UNDERGROUND", "Green Park", "London", "LIVE", "2026-08-19 09:00:00");
    }

    @Test
    void searchesFiltersAndPaginatesFramesWithStableOrdering() throws Exception {
        mockMvc.perform(get("/api/frames")
                .queryParam("q", "d6")
                .queryParam("status", "LIVE")
                .queryParam("mediaType", "DIGITAL")
                .queryParam("page", "0")
                .queryParam("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].frameId").value("FRAME-001"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void searchesAcrossLocationFieldsCaseInsensitively() throws Exception {
        mockMvc.perform(get("/api/frames").queryParam("q", "green PARK"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].frameId").value("FRAME-001"));
    }

    @Test
    void returnsFrameDetail() throws Exception {
        mockMvc.perform(get("/api/frames/FRAME-003"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.frameId").value("FRAME-003"))
            .andExpect(jsonPath("$.station").value("Paddington"))
            .andExpect(jsonPath("$.environment").value("RAIL"))
            .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void returnsProblemDetailWhenFrameDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/frames/UNKNOWN"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Frame not found"))
            .andExpect(jsonPath("$.detail").value("Frame 'UNKNOWN' was not found"));
    }

    @Test
    void rejectsOversizedPages() throws Exception {
        mockMvc.perform(get("/api/frames").queryParam("size", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid request parameters"));
    }

    @Test
    void rejectsUnknownFilterValuesWithProblemDetail() throws Exception {
        mockMvc.perform(get("/api/frames").queryParam("status", "UNKNOWN"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid request parameter"))
            .andExpect(jsonPath("$.detail").value("Parameter 'status' must be one of: LIVE, PENDING, MAINTENANCE, BLOCKED"));
    }

    @Test
    void configuresTheApplicationExecutorToUseVirtualThreads() throws Exception {
        assertThat(applicationTaskExecutor.submitCompletable(() -> Thread.currentThread().isVirtual()).get()).isTrue();
    }

    @Test
    void addsARequestIdToApiResponses() throws Exception {
        mockMvc.perform(get("/api/frames/FRAME-001"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Request-ID", matchesPattern(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
            )));
    }

    @Test
    void preservesValidRequestIdsAndReplacesUnsafeOnes() throws Exception {
        mockMvc.perform(get("/api/frames/FRAME-001")
                .header("X-Request-ID", "manual-check_123"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Request-ID", "manual-check_123"));

        mockMvc.perform(get("/api/frames/FRAME-001")
                .header("X-Request-ID", "unsafe request id"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Request-ID", matchesPattern(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
            )));
    }

    @Test
    void exposesLivenessAndDatabaseReadinessProbes() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void createsAFrameAndRecordsItsHistory() throws Exception {
        mockMvc.perform(post("/api/frames")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest("FRAME-004")))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", startsWith("http://localhost/api/frames/FRAME-004")))
            .andExpect(jsonPath("$.frameId").value("FRAME-004"))
            .andExpect(jsonPath("$.version").value(0));

        mockMvc.perform(get("/api/frames/FRAME-004/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].action").value("CREATED"))
            .andExpect(jsonPath("$[0].source").value("MANUAL"))
            .andExpect(jsonPath("$[0].actor").value("demo-user"))
            .andExpect(jsonPath("$[0].changes[0].fieldName").value("frameId"))
            .andExpect(jsonPath("$[0].changes[0].newValue").value("FRAME-004"));
    }

    @Test
    void rejectsDuplicateFrameIds() throws Exception {
        mockMvc.perform(post("/api/frames")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest("FRAME-001")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Frame already exists"));
    }

    @Test
    void treatsEquivalentDatabaseScaledCoordinatesAsUnchanged() throws Exception {
        mockMvc.perform(post("/api/frames")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest("FRAME-004")))
            .andExpect(status().isCreated());

        mockMvc.perform(put("/api/frames/FRAME-004")
                .contentType(MediaType.APPLICATION_JSON)
                .content(equivalentCreatedFrameUpdateRequest()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(0));

        mockMvc.perform(get("/api/frames/FRAME-004/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void updatesOnlyChangedFieldsAndRecordsTheirHistory() throws Exception {
        mockMvc.perform(put("/api/frames/FRAME-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest(0, "Piccadilly Circus", "MAINTENANCE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.station").value("Piccadilly Circus"))
            .andExpect(jsonPath("$.status").value("MAINTENANCE"))
            .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(get("/api/frames/FRAME-001/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].action").value("UPDATED"))
            .andExpect(jsonPath("$[0].changes", hasSize(2)))
            .andExpect(jsonPath("$[0].changes[0].fieldName").value("station"))
            .andExpect(jsonPath("$[0].changes[0].oldValue").value("Green Park"))
            .andExpect(jsonPath("$[0].changes[0].newValue").value("Piccadilly Circus"))
            .andExpect(jsonPath("$[0].changes[1].fieldName").value("status"));
    }

    @Test
    void doesNotCreateHistoryForAnIdenticalUpdate() throws Exception {
        mockMvc.perform(put("/api/frames/FRAME-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest(0, "Green Park", "LIVE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(0));

        mockMvc.perform(get("/api/frames/FRAME-001/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void returnsHistoryNewestFirst() throws Exception {
        mockMvc.perform(put("/api/frames/FRAME-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest(0, "First station", "LIVE")))
            .andExpect(status().isOk());
        mockMvc.perform(put("/api/frames/FRAME-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest(1, "Second station", "BLOCKED")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/frames/FRAME-001/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].changes[0].oldValue").value("First station"))
            .andExpect(jsonPath("$[0].changes[0].newValue").value("Second station"))
            .andExpect(jsonPath("$[1].changes[0].oldValue").value("Green Park"))
            .andExpect(jsonPath("$[1].changes[0].newValue").value("First station"));
    }

    @Test
    void rejectsStaleUpdatesWithoutChangingTheFrame() throws Exception {
        mockMvc.perform(put("/api/frames/FRAME-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest(9, "Stale station", "BLOCKED")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Frame was modified"));

        mockMvc.perform(get("/api/frames/FRAME-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.station").value("Green Park"))
            .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void allowsOnlyOneOfTwoConcurrentUpdatesWithTheSameVersion() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> performUpdateAfter(start, "Concurrent A"));
            var second = executor.submit(() -> performUpdateAfter(start, "Concurrent B"));
            start.countDown();

            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(200, 409);
        }

        mockMvc.perform(get("/api/frames/FRAME-001/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/frames/FRAME-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.station").value(anyOf(equalTo("Concurrent A"), equalTo("Concurrent B"))))
            .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void returnsProblemDetailForInvalidFrameData() throws Exception {
        mockMvc.perform(post("/api/frames")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"frameId":"","mediaType":"DIGITAL","environment":"RAIL","countryCode":"UK","status":"LIVE"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid frame"))
            .andExpect(jsonPath("$.errors.frameId").exists())
            .andExpect(jsonPath("$.errors.format").exists());
    }

    @Test
    void rejectsCoordinatePrecisionThatTheDatabaseWouldRound() throws Exception {
        mockMvc.perform(post("/api/frames")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest("FRAME-004").replace("-0.1757", "-0.175700001")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid frame"))
            .andExpect(jsonPath("$.errors.longitude").exists());
    }

    @Test
    void preservesEightDecimalCoordinatePrecision() throws Exception {
        mockMvc.perform(post("/api/frames")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest("FRAME-004")
                    .replace("-0.1757", "-0.17570001")
                    .replace("51.5154", "51.51540001")))
            .andExpect(status().isCreated());

        BigDecimal longitude = jdbcClient.sql("select longitude from frames where frame_id = 'FRAME-004'")
            .query(BigDecimal.class)
            .single();
        BigDecimal latitude = jdbcClient.sql("select latitude from frames where frame_id = 'FRAME-004'")
            .query(BigDecimal.class)
            .single();
        assertThat(longitude).isEqualByComparingTo("-0.17570001");
        assertThat(latitude).isEqualByComparingTo("51.51540001");
    }

    @Test
    void rejectsFrameIdsThatCannotBeUsedAsPathSegments() throws Exception {
        mockMvc.perform(post("/api/frames")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest("FRAME/004")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid frame"))
            .andExpect(jsonPath("$.errors.frameId").exists());
    }

    @Test
    void returnsProblemDetailForInvalidRequestBodyValues() throws Exception {
        mockMvc.perform(post("/api/frames")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest("FRAME-004").replace("DIGITAL", "SCREEN")))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Invalid frame"));
    }

    private void insertFrame(String id, String mediaType, String format, String environment,
                             String station, String town, String status, String updatedAt) {
        jdbcClient.sql("""
                insert into frames (
                    frame_id, media_type, format, environment, station, town, country_code,
                    status, premium, created_at, updated_at, version
                ) values (
                    :frameId, :mediaType, :format, :environment, :station, :town, 'UK',
                    :status, false, '2026-01-01 00:00:00', :updatedAt, 0
                )
                """)
            .param("frameId", id)
            .param("mediaType", mediaType)
            .param("format", format)
            .param("environment", environment)
            .param("station", station)
            .param("town", town)
            .param("status", status)
            .param("updatedAt", updatedAt)
            .update();
    }

    private int performUpdateAfter(CountDownLatch start, String station) throws Exception {
        start.await();
        return mockMvc.perform(put("/api/frames/FRAME-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest(0, station, "LIVE")))
            .andReturn()
            .getResponse()
            .getStatus();
    }

    private String createRequest(String frameId) {
        return """
            {
              "frameId": "%s",
              "mediaType": "DIGITAL",
              "format": "D6",
              "environment": "RAIL",
              "siteNumber": "SITE-1",
              "station": "Paddington",
              "address": "Praed Street",
              "region": "London",
              "countryCode": "UK",
              "town": "London",
              "postcode": "W2 1HQ",
              "longitude": -0.1757,
              "latitude": 51.5154,
              "status": "LIVE",
              "numberOfSlots": 6,
              "distanceToClosestSchool": 500,
              "pixelHeight": 1920,
              "pixelWidth": 1080,
              "premium": false
            }
            """.formatted(frameId);
    }

    private String updateRequest(long version, String station, String status) {
        return """
            {
              "version": %d,
              "mediaType": "DIGITAL",
              "format": "D6",
              "environment": "UNDERGROUND",
              "station": "%s",
              "countryCode": "UK",
              "town": "London",
              "status": "%s",
              "premium": false
            }
            """.formatted(version, station, status);
    }

    private String equivalentCreatedFrameUpdateRequest() {
        return """
            {
              "version": 0,
              "mediaType": "DIGITAL",
              "format": "D6",
              "environment": "RAIL",
              "siteNumber": "SITE-1",
              "station": "Paddington",
              "address": "Praed Street",
              "region": "London",
              "countryCode": "UK",
              "town": "London",
              "postcode": "W2 1HQ",
              "longitude": -0.1757000,
              "latitude": 51.5154000,
              "status": "LIVE",
              "numberOfSlots": 6,
              "distanceToClosestSchool": 500,
              "pixelHeight": 1920,
              "pixelWidth": 1080,
              "premium": false
            }
            """;
    }
}
