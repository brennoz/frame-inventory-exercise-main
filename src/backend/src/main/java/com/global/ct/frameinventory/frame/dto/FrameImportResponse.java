package com.global.ct.frameinventory.frame.dto;

import java.util.List;

public record FrameImportResponse(
    int created,
    int updated,
    int unchanged,
    int failed,
    List<FrameImportErrorResponse> errors
) {
}
