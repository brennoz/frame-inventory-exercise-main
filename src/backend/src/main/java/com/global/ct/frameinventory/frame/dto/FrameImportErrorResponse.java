package com.global.ct.frameinventory.frame.dto;

public record FrameImportErrorResponse(long rowNumber, String frameId, String message) {
}
