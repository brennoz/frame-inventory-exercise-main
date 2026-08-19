package com.global.ct.frameinventory.frame.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.global.ct.frameinventory.DatabaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.jdbc.core.simple.JdbcClient;
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
}
