package com.global.ct.frameinventory.frame.exception;

public class FrameNotFoundException extends RuntimeException {

    public FrameNotFoundException(String frameId) {
        super("Frame '" + frameId + "' was not found");
    }
}
