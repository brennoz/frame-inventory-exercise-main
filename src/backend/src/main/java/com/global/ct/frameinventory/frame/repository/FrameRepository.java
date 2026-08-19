package com.global.ct.frameinventory.frame.repository;

import com.global.ct.frameinventory.frame.model.Frame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FrameRepository extends JpaRepository<Frame, String>, JpaSpecificationExecutor<Frame> {
}
