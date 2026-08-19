package com.global.ct.frameinventory.frame.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "frame_revision_changes")
public class FrameRevisionChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revision_id", nullable = false, updatable = false)
    private FrameRevision revision;

    @Column(name = "field_name", length = 64, nullable = false, updatable = false)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "text", updatable = false)
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "text", updatable = false)
    private String newValue;

    protected FrameRevisionChange() {
    }

    static FrameRevisionChange create(FrameRevision revision, FrameFieldChange change) {
        FrameRevisionChange entity = new FrameRevisionChange();
        entity.revision = revision;
        entity.fieldName = change.fieldName();
        entity.oldValue = change.oldValue();
        entity.newValue = change.newValue();
        return entity;
    }

    public String getFieldName() { return fieldName; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
}
