package com.global.ct.frameinventory.frame.repository;

import com.global.ct.frameinventory.frame.model.FrameRevision;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FrameRevisionRepository extends JpaRepository<FrameRevision, Long> {

    @EntityGraph(attributePaths = "changes")
    List<FrameRevision> findByFrameFrameIdOrderByOccurredAtDescIdDesc(String frameId);
}
