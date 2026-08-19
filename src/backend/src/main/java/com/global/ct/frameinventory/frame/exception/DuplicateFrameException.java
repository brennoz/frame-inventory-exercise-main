package com.global.ct.frameinventory.frame.exception;

public class DuplicateFrameException extends RuntimeException {
    public DuplicateFrameException(String frameId) {
        super("Frame '" + frameId + "' already exists");
    }
}
