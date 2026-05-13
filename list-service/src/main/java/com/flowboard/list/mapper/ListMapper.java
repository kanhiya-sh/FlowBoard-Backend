package com.flowboard.list.mapper;

import com.flowboard.list.dto.ListResponseDTO;
import com.flowboard.list.entity.TaskList;

public class ListMapper {
    private ListMapper() {}
    public static ListResponseDTO toResponseDTO(TaskList list) {
        return ListResponseDTO.builder()
                .listId(list.getListId())
                .boardId(list.getBoardId())
                .name(list.getName())
                .position(list.getPosition())
                .color(list.getColor())
                .isArchived(list.getIsArchived())
                .createdAt(list.getCreatedAt())
                .updatedAt(list.getUpdatedAt())
                .build();
    }
}
