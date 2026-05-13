package com.flowboard.board.mapper;

import com.flowboard.board.dto.*;
import com.flowboard.board.entity.Board;

public class BoardMapper {
    public static Board toEntity(BoardRequestDTO dto) {
        return Board.builder()
                .workspaceId(dto.getWorkspaceId())
                .name(dto.getName())
                .description(dto.getDescription())
                .background(dto.getBackground())
                .visibility(dto.getVisibility())
                .build();
    }
    public static BoardResponseDTO toResponseDTO(Board board) {
        return BoardResponseDTO.builder()
                .boardId(board.getBoardId())
                .workspaceId(board.getWorkspaceId())
                .name(board.getName())
                .description(board.getDescription())
                .background(board.getBackground())
                .visibility(board.getVisibility())
                .createdById(board.getCreatedById())
                .isClosed(board.getIsClosed())
                .createdAt(board.getCreatedAt())
                .build();
    }
}