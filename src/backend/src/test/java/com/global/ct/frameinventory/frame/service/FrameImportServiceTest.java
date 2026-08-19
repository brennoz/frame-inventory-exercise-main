package com.global.ct.frameinventory.frame.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.global.ct.frameinventory.frame.dto.FrameImportResponse;
import com.global.ct.frameinventory.frame.exception.InvalidCsvException;
import com.global.ct.frameinventory.frame.exception.InvalidCsvRowException;
import com.global.ct.frameinventory.frame.model.FrameData;
import com.global.ct.frameinventory.frame.model.FrameEnvironment;
import com.global.ct.frameinventory.frame.model.FrameImportOutcome;
import com.global.ct.frameinventory.frame.model.FrameStatus;
import com.global.ct.frameinventory.frame.model.MediaType;
import java.nio.charset.StandardCharsets;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class FrameImportServiceTest {

    private static final String HEADER = """
        frame_id,type_classic_digital,format,environment,site_no,station,address,region,country_code,town,postcode,longitude,latitude,status,status_reason,number_of_slots,distance_to_closest_school,pixel_height,pixel_width,premium
        """;
    private static final String ROW = "CSV-001,DIGITAL,D6,RAIL,SITE-1,Paddington,Praed Street,London,UK,London,W2 1HQ,-0.17570001,51.51540001,LIVE,,6,500,1200,1920,0\n";

    @Mock
    private FrameCsvMapper mapper;

    @Mock
    private FrameCommandService commandService;

    private FrameImportService importService;

    @BeforeEach
    void setUp() {
        importService = new FrameImportService(mapper, commandService);
    }

    @Test
    void rejectsMoreThanFiveThousandRowsBeforeWritingAnyFrame() {
        assertThatThrownBy(() -> importService.importCsv(csvWithRows(5_001)))
            .isInstanceOf(InvalidCsvException.class)
            .hasMessageContaining("5,000");

        verifyNoInteractions(commandService);
    }

    @Test
    void capsReportedErrorsWhileRetainingTheTotalFailureCount() {
        when(mapper.map(any(CSVRecord.class))).thenThrow(new InvalidCsvRowException("invalid row"));

        FrameImportResponse response = importService.importCsv(csvWithRows(101));

        assertThat(response.failed()).isEqualTo(101);
        assertThat(response.errors()).hasSize(100);
        assertThat(response.errorsTruncated()).isTrue();
    }

    @Test
    void propagatesDatabaseInfrastructureFailures() {
        stubValidRow();
        DataAccessResourceFailureException failure = new DataAccessResourceFailureException("database unavailable");
        when(commandService.upsertImportedFrame(any(), any())).thenThrow(failure);

        assertThatThrownBy(() -> importService.importCsv(csvWithRows(1)))
            .isSameAs(failure);
    }

    private void stubValidRow() {
        FrameData data = new FrameData(
            MediaType.DIGITAL, "D6", FrameEnvironment.RAIL, "SITE-1", "Paddington", "Praed Street",
            "London", "UK", "London", "W2 1HQ", null, null, FrameStatus.LIVE, null,
            6, 500, 1200, 1920, false
        );
        when(mapper.map(any(CSVRecord.class))).thenReturn(new FrameImportRow("CSV-001", data));
        when(commandService.upsertImportedFrame(any(), any())).thenReturn(FrameImportOutcome.UNCHANGED);
    }

    private MockMultipartFile csvWithRows(int rowCount) {
        StringBuilder csv = new StringBuilder(HEADER.length() + ROW.length() * rowCount);
        csv.append(HEADER);
        csv.append(ROW.repeat(rowCount));
        return new MockMultipartFile(
            "file", "frames.csv", "text/csv", csv.toString().getBytes(StandardCharsets.UTF_8)
        );
    }
}
