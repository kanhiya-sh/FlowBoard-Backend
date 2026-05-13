package com.flowboard.board.repository;

import com.flowboard.board.entity.BoardMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardMemberRepository extends JpaRepository<BoardMember, Long> {

    List<BoardMember> findByBoardId(Long boardId);

    Optional<BoardMember> findByBoardIdAndUserId(Long boardId, Long userId);

    List<BoardMember> findByUserId(Long userId);
}