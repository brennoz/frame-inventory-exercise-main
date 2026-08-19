package com.global.ct.frameinventory.frame.service;

import com.global.ct.frameinventory.frame.dto.FrameResponse;
import com.global.ct.frameinventory.frame.dto.PageResponse;
import com.global.ct.frameinventory.frame.exception.FrameNotFoundException;
import com.global.ct.frameinventory.frame.model.FrameEnvironment;
import com.global.ct.frameinventory.frame.model.FrameStatus;
import com.global.ct.frameinventory.frame.model.MediaType;
import com.global.ct.frameinventory.frame.repository.FrameRepository;
import com.global.ct.frameinventory.frame.repository.FrameSpecifications;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FrameQueryService {

    private static final Sort DEFAULT_SORT = Sort.by(
        Sort.Order.desc("updatedAt"),
        Sort.Order.asc("frameId")
    );

    private final FrameRepository repository;

    public FrameQueryService(FrameRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<FrameResponse> search(String query, FrameStatus status, FrameEnvironment environment,
                                              MediaType mediaType, int page, int size) {
        var pageRequest = PageRequest.of(page, size, DEFAULT_SORT);
        var frames = repository.findAll(FrameSpecifications.matching(query, status, environment, mediaType), pageRequest);
        return PageResponse.from(frames, FrameResponse::from);
    }

    @Transactional(readOnly = true)
    public FrameResponse get(String frameId) {
        return repository.findById(frameId)
            .map(FrameResponse::from)
            .orElseThrow(() -> new FrameNotFoundException(frameId));
    }
}
