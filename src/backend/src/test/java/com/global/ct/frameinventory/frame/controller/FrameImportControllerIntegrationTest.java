package com.global.ct.frameinventory.frame.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.global.ct.frameinventory.DatabaseIntegrationTest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FrameImportControllerIntegrationTest extends DatabaseIntegrationTest {

    private static final String HEADER = """
        frame_id,type_classic_digital,format,environment,site_no,station,address,region,country_code,town,postcode,longitude,latitude,status,status_reason,number_of_slots,distance_to_closest_school,pixel_height,pixel_width,premium
        """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("delete from frame_revision_changes").update();
        jdbcClient.sql("delete from frame_revisions").update();
        jdbcClient.sql("delete from frames").update();
    }

    @Test
    void importsQuotedFieldsAndRecordsCsvHistory() throws Exception {
        String csv = HEADER + row("CSV-001", "Paddington", "\"Platform 1, main concourse\"", "LIVE");

        mockMvc.perform(multipart("/api/frames/import").file(csvFile(csv)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.created").value(1))
            .andExpect(jsonPath("$.updated").value(0))
            .andExpect(jsonPath("$.unchanged").value(0))
            .andExpect(jsonPath("$.failed").value(0))
            .andExpect(jsonPath("$.errorsTruncated").value(false))
            .andExpect(jsonPath("$.errors", hasSize(0)));

        String address = jdbcClient.sql("select address from frames where frame_id = 'CSV-001'")
            .query(String.class)
            .single();
        assertThat(address).isEqualTo("Platform 1, main concourse");

        mockMvc.perform(get("/api/frames/CSV-001/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].source").value("CSV"))
            .andExpect(jsonPath("$[0].actor").value("csv-import"));
    }

    @Test
    void upsertsRowsAndKeepsSuccessfulRowsWhenOneFails() throws Exception {
        String initialCsv = HEADER
            + row("CSV-001", "Paddington", "Praed Street", "LIVE")
            + row("CSV-002", "Waterloo", "York Road", "LIVE");
        mockMvc.perform(multipart("/api/frames/import").file(csvFile(initialCsv)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.created").value(2));

        String upsertCsv = HEADER
            + row("CSV-001", "Paddington Elizabeth line", "Praed Street", "LIVE")
            + rowWithMediaType("CSV-BAD", "SCREEN")
            + row("CSV-002", "Waterloo", "York Road", "LIVE")
            + row("CSV-003", "Victoria", "Victoria Street", "LIVE");

        mockMvc.perform(multipart("/api/frames/import").file(csvFile(upsertCsv)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.created").value(1))
            .andExpect(jsonPath("$.updated").value(1))
            .andExpect(jsonPath("$.unchanged").value(1))
            .andExpect(jsonPath("$.failed").value(1))
            .andExpect(jsonPath("$.errors", hasSize(1)))
            .andExpect(jsonPath("$.errors[0].rowNumber").value(3))
            .andExpect(jsonPath("$.errors[0].frameId").value("CSV-BAD"));

        assertThat(jdbcClient.sql("select count(*) from frames").query(Long.class).single()).isEqualTo(3);
        assertThat(jdbcClient.sql("select station from frames where frame_id = 'CSV-001'")
            .query(String.class).single()).isEqualTo("Paddington Elizabeth line");
        assertThat(jdbcClient.sql("select count(*) from frame_revisions where frame_id = 'CSV-001'")
            .query(Long.class).single()).isEqualTo(2);
        assertThat(jdbcClient.sql("select count(*) from frame_revisions where frame_id = 'CSV-002'")
            .query(Long.class).single()).isEqualTo(1);

        mockMvc.perform(get("/api/frames/CSV-001/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].action").value("UPDATED"))
            .andExpect(jsonPath("$[0].source").value("CSV"))
            .andExpect(jsonPath("$[0].actor").value("csv-import"))
            .andExpect(jsonPath("$[0].changes[0].fieldName").value("station"));
    }

    @Test
    void rejectsCsvFilesMissingRequiredHeaders() throws Exception {
        String csv = "frame_id,type_classic_digital,format,environment,country_code,premium\n"
            + "CSV-001,DIGITAL,D6,RAIL,UK,0\n";

        mockMvc.perform(multipart("/api/frames/import").file(csvFile(csv)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid CSV"))
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Missing required headers")));

        assertThat(jdbcClient.sql("select count(*) from frames").query(Long.class).single()).isZero();
    }

    @Test
    void rejectsMissingCsvFilesWithProblemDetail() throws Exception {
        mockMvc.perform(multipart("/api/frames/import"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid CSV"));
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
            "file", "frames.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String row(String frameId, String station, String address, String status) {
        return "%s,DIGITAL,D6,RAIL,SITE-1,%s,%s,London,UK,London,W2 1HQ,-0.17570001,51.51540001,%s,,6,500,1200,1920,0%n"
            .formatted(frameId, station, address, status);
    }

    private String rowWithMediaType(String frameId, String mediaType) {
        return "%s,%s,D6,RAIL,SITE-1,Paddington,Praed Street,London,UK,London,W2 1HQ,-0.17570001,51.51540001,LIVE,,6,500,1200,1920,0%n"
            .formatted(frameId, mediaType);
    }
}
