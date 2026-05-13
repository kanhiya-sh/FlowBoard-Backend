package com.flowboard.board.repository;

import com.flowboard.board.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {

    List<Board> findByWorkspaceId(Long workspaceId);

    List<Board> findByCreatedById(Long userId);

    List<Board> findByIsClosed(Boolean isClosed);
}