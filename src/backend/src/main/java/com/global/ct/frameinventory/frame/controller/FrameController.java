package com.global.ct.frameinventory.frame.controller;

import com.global.ct.frameinventory.frame.dto.FrameResponse;
import com.global.ct.frameinventory.frame.dto.PageResponse;
import com.global.ct.frameinventory.frame.model.FrameEnvironment;
import com.global.ct.frameinventory.frame.model.FrameStatus;
import com.global.ct.frameinventory.frame.model.MediaType;
import com.global.ct.frameinventory.frame.service.FrameQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/frames")
@Validated
public class FrameController {

    private final FrameQueryService queryService;

    public FrameController(FrameQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public PageResponse<FrameResponse> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) FrameStatus status,
        @RequestParam(required = false) FrameEnvironment environment,
        @RequestParam(required = false) MediaType mediaType,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size
    ) {
        return queryService.search(q, status, environment, mediaType, page, size);
    }

    @GetMapping("/{frameId}")
    public FrameResponse get(@PathVariable String frameId) {
        return queryService.get(frameId);
    }
}
