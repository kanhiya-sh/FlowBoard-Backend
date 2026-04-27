package com.flowboard.board.controller;

import com.flowboard.board.dto.*;
import com.flowboard.board.enums.BoardRole;
import com.flowboard.board.mapper.BoardMapper;
import com.flowboard.board.service.BoardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    private String getEmail(HttpServletRequest request) {
        String email = (String) request.getAttribute("userEmail");
        if (email == null) throw new IllegalStateException("User not authenticated");
        return email;
    }

    @PostMapping
    public ResponseEntity<BoardResponseDTO> createBoard(
            @Valid @RequestBody BoardRequestDTO dto,
            HttpServletRequest request) {
        var saved = boardService.createBoard(dto, getEmail(request));
        return ResponseEntity.ok(BoardMapper.toResponseDTO(saved));
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<BoardResponseDTO> getBoardById(@PathVariable Long boardId) {
        return ResponseEntity.ok(BoardMapper.toResponseDTO(boardService.getBoardById(boardId)));
    }

    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<BoardResponseDTO>> getBoardsByWorkspace(@PathVariable Long workspaceId) {
        return ResponseEntity.ok(boardService.getBoardsByWorkspace(workspaceId)
                .stream().map(BoardMapper::toResponseDTO).toList());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BoardResponseDTO>> getBoardsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(boardService.getBoardsByUser(userId)
                .stream().map(BoardMapper::toResponseDTO).toList());
    }

    @PutMapping("/{boardId}")
    public ResponseEntity<BoardResponseDTO> updateBoard(
            @PathVariable Long boardId,
            @Valid @RequestBody BoardRequestDTO dto,
            HttpServletRequest request) {
        var updated = boardService.updateBoard(boardId, dto, getEmail(request));
        return ResponseEntity.ok(BoardMapper.toResponseDTO(updated));
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<String> deleteBoard(
            @PathVariable Long boardId,
            HttpServletRequest request) {
        boardService.deleteBoard(boardId, getEmail(request));
        return ResponseEntity.ok("Board deleted successfully");
    }

    @PutMapping("/{boardId}/close")
    public ResponseEntity<BoardResponseDTO> closeBoard(
            @PathVariable Long boardId,
            HttpServletRequest request) {
        return ResponseEntity.ok(BoardMapper.toResponseDTO(boardService.closeBoard(boardId, getEmail(request))));
    }

    @PostMapping("/{boardId}/members")
    public ResponseEntity<BoardMemberResponseDTO> addMember(
            @PathVariable Long boardId,
            @RequestParam Long userId,
            @RequestParam BoardRole role,
            HttpServletRequest request) {
        return ResponseEntity.ok(boardService.addMember(boardId, userId, role, getEmail(request)));
    }

    @DeleteMapping("/{boardId}/members/{userId}")
    public ResponseEntity<String> removeMember(
            @PathVariable Long boardId,
            @PathVariable Long userId,
            HttpServletRequest request) {
        boardService.removeMember(boardId, userId, getEmail(request));
        return ResponseEntity.ok("Member removed successfully");
    }

    @PutMapping("/{boardId}/members/{userId}/role")
    public ResponseEntity<BoardMemberResponseDTO> updateMemberRole(
            @PathVariable Long boardId,
            @PathVariable Long userId,
            @RequestParam BoardRole role,
            HttpServletRequest request) {
        return ResponseEntity.ok(boardService.updateMemberRole(boardId, userId, role, getEmail(request)));
    }

    @GetMapping("/{boardId}/members")
    public ResponseEntity<List<BoardMemberResponseDTO>> getMembers(@PathVariable Long boardId) {
        return ResponseEntity.ok(boardService.getBoardMembers(boardId));
    }

    // Internal Endpoint (called by list-service and card-service — no JWT needed)
    @GetMapping("/internal/{boardId}/members/{userId}/check")
    public ResponseEntity<BoardMemberCheckResponseDTO> checkBoardMembership(
            @PathVariable Long boardId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(boardService.checkBoardMembership(boardId, userId));
    }
}