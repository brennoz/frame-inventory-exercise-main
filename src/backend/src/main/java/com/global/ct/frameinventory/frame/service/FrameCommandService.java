package com.global.ct.frameinventory.frame.service;

import com.global.ct.frameinventory.frame.dto.CreateFrameRequest;
import com.global.ct.frameinventory.frame.dto.FrameResponse;
import com.global.ct.frameinventory.frame.dto.UpdateFrameRequest;
import com.global.ct.frameinventory.frame.exception.DuplicateFrameException;
import com.global.ct.frameinventory.frame.exception.FrameNotFoundException;
import com.global.ct.frameinventory.frame.exception.StaleFrameVersionException;
import com.global.ct.frameinventory.frame.model.ChangeSource;
import com.global.ct.frameinventory.frame.model.Frame;
import com.global.ct.frameinventory.frame.model.FrameData;
import com.global.ct.frameinventory.frame.model.FrameFieldChange;
import com.global.ct.frameinventory.frame.model.FrameImportOutcome;
import com.global.ct.frameinventory.frame.model.FrameRevision;
import com.global.ct.frameinventory.frame.model.RevisionAction;
import com.global.ct.frameinventory.frame.repository.FrameRepository;
import com.global.ct.frameinventory.frame.repository.FrameRevisionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FrameCommandService {

    private static final String MANUAL_ACTOR = "demo-user";

    private final FrameRepository frameRepository;
    private final FrameRevisionRepository revisionRepository;
    private final Clock clock;

    public FrameCommandService(FrameRepository frameRepository, FrameRevisionRepository revisionRepository, Clock clock) {
        this.frameRepository = frameRepository;
        this.revisionRepository = revisionRepository;
        this.clock = clock;
    }

    @Transactional
    public FrameResponse create(CreateFrameRequest request) {
        if (frameRepository.existsById(request.frameId())) {
            throw new DuplicateFrameException(request.frameId());
        }

        Instant now = now();
        Frame frame = Frame.create(request.frameId(), request.toData(), now);
        try {
            frameRepository.saveAndFlush(frame);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateFrameException(request.frameId());
        }

        List<FrameFieldChange> changes = new ArrayList<>();
        changes.add(new FrameFieldChange("frameId", null, frame.getFrameId()));
        changes.addAll(frame.data().changesFrom(null));
        revisionRepository.save(FrameRevision.record(
            frame, RevisionAction.CREATED, ChangeSource.MANUAL, MANUAL_ACTOR, now, changes
        ));
        return FrameResponse.from(frame);
    }

    @Transactional
    public FrameResponse update(String frameId, UpdateFrameRequest request) {
        Frame frame = frameRepository.findById(frameId)
            .orElseThrow(() -> new FrameNotFoundException(frameId));
        if (frame.getVersion() != request.version()) {
            throw new StaleFrameVersionException(frameId);
        }

        Instant now = now();
        List<FrameFieldChange> changes = frame.update(request.toData(), now);
        if (changes.isEmpty()) {
            return FrameResponse.from(frame);
        }

        try {
            frameRepository.flush();
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new StaleFrameVersionException(frameId);
        }
        revisionRepository.saveAndFlush(FrameRevision.record(
            frame, RevisionAction.UPDATED, ChangeSource.MANUAL, MANUAL_ACTOR, now, changes
        ));
        return FrameResponse.from(frame);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FrameImportOutcome upsertImportedFrame(String frameId, FrameData data) {
        Frame frame = frameRepository.findById(frameId).orElse(null);
        Instant now = now();
        if (frame == null) {
            frame = Frame.create(frameId, data, now);
            frameRepository.saveAndFlush(frame);
            List<FrameFieldChange> changes = new ArrayList<>();
            changes.add(new FrameFieldChange("frameId", null, frameId));
            changes.addAll(data.changesFrom(null));
            revisionRepository.saveAndFlush(FrameRevision.record(
                frame, RevisionAction.CREATED, ChangeSource.CSV, "csv-import", now, changes
            ));
            return FrameImportOutcome.CREATED;
        }

        List<FrameFieldChange> changes = frame.update(data, now);
        if (changes.isEmpty()) {
            return FrameImportOutcome.UNCHANGED;
        }
        frameRepository.flush();
        revisionRepository.saveAndFlush(FrameRevision.record(
            frame, RevisionAction.UPDATED, ChangeSource.CSV, "csv-import", now, changes
        ));
        return FrameImportOutcome.UPDATED;
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
