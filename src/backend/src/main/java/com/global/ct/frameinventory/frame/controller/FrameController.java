package com.global.ct.frameinventory.frame.controller;

import com.global.ct.frameinventory.frame.dto.CreateFrameRequest;
import com.global.ct.frameinventory.frame.dto.FrameResponse;
import com.global.ct.frameinventory.frame.dto.FrameRevisionResponse;
import com.global.ct.frameinventory.frame.dto.PageResponse;
import com.global.ct.frameinventory.frame.dto.UpdateFrameRequest;
import com.global.ct.frameinventory.frame.model.FrameEnvironment;
import com.global.ct.frameinventory.frame.model.FrameStatus;
import com.global.ct.frameinventory.frame.model.MediaType;
import com.global.ct.frameinventory.frame.service.FrameCommandService;
import com.global.ct.frameinventory.frame.service.FrameHistoryService;
import com.global.ct.frameinventory.frame.service.FrameQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/frames")
@Validated
public class FrameController {

    private final FrameQueryService queryService;
    private final FrameCommandService commandService;
    private final FrameHistoryService historyService;

    public FrameController(FrameQueryService queryService, FrameCommandService commandService,
                           FrameHistoryService historyService) {
        this.queryService = queryService;
        this.commandService = commandService;
        this.historyService = historyService;
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

    @PostMapping
    public ResponseEntity<FrameResponse> create(@Valid @RequestBody CreateFrameRequest request) {
        FrameResponse frame = commandService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{frameId}")
            .buildAndExpand(frame.frameId())
            .toUri();
        return ResponseEntity.created(location).body(frame);
    }

    @PutMapping("/{frameId}")
    public FrameResponse update(@PathVariable String frameId,
                                @Valid @RequestBody UpdateFrameRequest request) {
        return commandService.update(frameId, request);
    }

    @GetMapping("/{frameId}/history")
    public List<FrameRevisionResponse> history(@PathVariable String frameId) {
        return historyService.get(frameId);
    }
}
