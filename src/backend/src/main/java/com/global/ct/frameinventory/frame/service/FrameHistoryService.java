package com.global.ct.frameinventory.frame.service;

import com.global.ct.frameinventory.frame.dto.FrameRevisionResponse;
import com.global.ct.frameinventory.frame.exception.FrameNotFoundException;
import com.global.ct.frameinventory.frame.repository.FrameRepository;
import com.global.ct.frameinventory.frame.repository.FrameRevisionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FrameHistoryService {

    private final FrameRepository frameRepository;
    private final FrameRevisionRepository revisionRepository;

    public FrameHistoryService(FrameRepository frameRepository, FrameRevisionRepository revisionRepository) {
        this.frameRepository = frameRepository;
        this.revisionRepository = revisionRepository;
    }

    @Transactional(readOnly = true)
    public List<FrameRevisionResponse> get(String frameId) {
        if (!frameRepository.existsById(frameId)) {
            throw new FrameNotFoundException(frameId);
        }
        return revisionRepository.findByFrameFrameIdOrderByOccurredAtDescIdDesc(frameId).stream()
            .map(FrameRevisionResponse::from)
            .toList();
    }
}
