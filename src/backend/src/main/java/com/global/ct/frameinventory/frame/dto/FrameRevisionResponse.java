package com.global.ct.frameinventory.frame.dto;

import com.global.ct.frameinventory.frame.model.ChangeSource;
import com.global.ct.frameinventory.frame.model.FrameRevision;
import com.global.ct.frameinventory.frame.model.RevisionAction;
import java.time.Instant;
import java.util.List;

public record FrameRevisionResponse(
    long id,
    RevisionAction action,
    ChangeSource source,
    String actor,
    Instant occurredAt,
    List<FrameChangeResponse> changes
) {
    public static FrameRevisionResponse from(FrameRevision revision) {
        return new FrameRevisionResponse(
            revision.getId(), revision.getAction(), revision.getSource(), revision.getActor(),
            revision.getOccurredAt(), revision.getChanges().stream().map(FrameChangeResponse::from).toList()
        );
    }
}
