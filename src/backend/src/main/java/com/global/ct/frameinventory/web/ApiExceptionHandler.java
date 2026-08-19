package com.global.ct.frameinventory.web;

import com.global.ct.frameinventory.frame.exception.DuplicateFrameException;
import com.global.ct.frameinventory.frame.exception.FrameNotFoundException;
import com.global.ct.frameinventory.frame.exception.StaleFrameVersionException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(FrameNotFoundException.class)
    ProblemDetail handleNotFound(FrameNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Frame not found");
        problem.setType(URI.create("urn:problem:frame-not-found"));
        return problem;
    }

    @ExceptionHandler(DuplicateFrameException.class)
    ProblemDetail handleDuplicate(DuplicateFrameException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Frame already exists");
        problem.setType(URI.create("urn:problem:duplicate-frame"));
        return problem;
    }

    @ExceptionHandler({StaleFrameVersionException.class, ObjectOptimisticLockingFailureException.class})
    ProblemDetail handleStaleVersion(RuntimeException exception) {
        String detail = exception instanceof StaleFrameVersionException
            ? exception.getMessage()
            : "The frame was modified by another request; reload it and try again";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
        problem.setTitle("Frame was modified");
        problem.setType(URI.create("urn:problem:stale-frame-version"));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleInvalidFrame(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
            errors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "One or more frame fields are invalid"
        );
        problem.setTitle("Invalid frame");
        problem.setType(URI.create("urn:problem:invalid-frame"));
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        return invalidParameters(exception.getMessage());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail handleMethodValidation(HandlerMethodValidationException exception) {
        return invalidParameters("One or more request parameters are invalid");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String detail = "Parameter '" + exception.getName() + "' has an invalid value";
        if (exception.getRequiredType() != null && exception.getRequiredType().isEnum()) {
            String values = Arrays.stream(exception.getRequiredType().getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
            detail = "Parameter '" + exception.getName() + "' must be one of: " + values;
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Invalid request parameter");
        problem.setType(URI.create("urn:problem:invalid-request-parameter"));
        return problem;
    }

    private ProblemDetail invalidParameters(String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Invalid request parameters");
        problem.setType(URI.create("urn:problem:invalid-request-parameters"));
        return problem;
    }
}
