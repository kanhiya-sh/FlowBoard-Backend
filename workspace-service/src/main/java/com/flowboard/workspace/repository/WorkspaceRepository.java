package com.flowboard.workspace.repository;

import com.flowboard.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    List<Workspace> findByOwnerId(Long ownerId);
    boolean existsByNameAndOwnerId(String name, Long ownerId);
}
