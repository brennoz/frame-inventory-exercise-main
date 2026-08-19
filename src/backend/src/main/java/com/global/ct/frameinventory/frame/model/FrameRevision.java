package com.global.ct.frameinventory.frame.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "frame_revisions")
public class FrameRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "frame_id", nullable = false, updatable = false)
    private Frame frame;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 32, nullable = false, updatable = false)
    private RevisionAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 32, nullable = false, updatable = false)
    private ChangeSource source;

    @Column(name = "actor", length = 100, nullable = false, updatable = false)
    private String actor;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @OneToMany(mappedBy = "revision", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<FrameRevisionChange> changes = new ArrayList<>();

    protected FrameRevision() {
    }

    public static FrameRevision record(Frame frame, RevisionAction action, ChangeSource source,
                                       String actor, Instant occurredAt, List<FrameFieldChange> fieldChanges) {
        FrameRevision revision = new FrameRevision();
        revision.frame = frame;
        revision.action = action;
        revision.source = source;
        revision.actor = actor;
        revision.occurredAt = occurredAt;
        fieldChanges.forEach(change -> revision.changes.add(FrameRevisionChange.create(revision, change)));
        return revision;
    }

    public Long getId() { return id; }
    public RevisionAction getAction() { return action; }
    public ChangeSource getSource() { return source; }
    public String getActor() { return actor; }
    public Instant getOccurredAt() { return occurredAt; }
    public List<FrameRevisionChange> getChanges() { return List.copyOf(changes); }
}
