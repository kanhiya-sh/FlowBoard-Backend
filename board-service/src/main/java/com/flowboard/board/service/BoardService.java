package com.flowboard.board.service;

import com.flowboard.board.dto.BoardMemberResponseDTO;
import com.flowboard.board.dto.BoardRequestDTO;
import com.flowboard.board.entity.Board;
import com.flowboard.board.enums.BoardRole;
import com.flowboard.board.dto.BoardMemberCheckResponseDTO;
import java.util.List;

public interface BoardService {
    Board createBoard(BoardRequestDTO dto, String userEmail);
    Board getBoardById(Long boardId);
    List<Board> getBoardsByWorkspace(Long workspaceId);
    List<Board> getBoardsByUser(Long userId);
    Board updateBoard(Long boardId, BoardRequestDTO dto, String userEmail);
    void deleteBoard(Long boardId, String userEmail);
    Board closeBoard(Long boardId, String userEmail);
    BoardMemberResponseDTO addMember(Long boardId, Long userId, BoardRole role, String userEmail);
    void removeMember(Long boardId, Long userId, String userEmail);
    BoardMemberResponseDTO updateMemberRole(Long boardId, Long userId, BoardRole role, String userEmail);
    List<BoardMemberResponseDTO> getBoardMembers(Long boardId);
    BoardMemberCheckResponseDTO checkBoardMembership(Long boardId, Long userId);
}
