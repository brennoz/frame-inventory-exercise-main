package com.global.ct.frameinventory.frame.dto;

import com.global.ct.frameinventory.frame.model.FrameRevisionChange;

public record FrameChangeResponse(String fieldName, String oldValue, String newValue) {
    static FrameChangeResponse from(FrameRevisionChange change) {
        return new FrameChangeResponse(change.getFieldName(), change.getOldValue(), change.getNewValue());
    }
}
