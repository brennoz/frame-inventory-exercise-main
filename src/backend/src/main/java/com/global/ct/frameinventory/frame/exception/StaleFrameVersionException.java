package com.global.ct.frameinventory.frame.exception;

public class StaleFrameVersionException extends RuntimeException {
    public StaleFrameVersionException(String frameId) {
        super("Frame '" + frameId + "' was modified by another request; reload it and try again");
    }
}
