package com.flowboard.label.mapper;

import com.flowboard.label.dto.*;
import com.flowboard.label.entity.*;
import java.util.List;

public class LabelMapper {
    private LabelMapper() {}

    public static LabelResponseDTO toResponseDTO(Label label) {
        return LabelResponseDTO.builder()
                .labelId(label.getLabelId())
                .boardId(label.getBoardId())
                .name(label.getName())
                .color(label.getColor())
                .createdAt(label.getCreatedAt())
                .build();
    }
    public static ChecklistItemResponseDTO toResponseDTO(ChecklistItem item) {
        return ChecklistItemResponseDTO.builder()
                .itemId(item.getItemId())
                .checklistId(item.getChecklistId())
                .text(item.getText())
                .isCompleted(item.getIsCompleted())
                .assigneeId(item.getAssigneeId())
                .dueDate(item.getDueDate())
                .build();
    }
    public static ChecklistResponseDTO toResponseDTO(Checklist checklist, List<ChecklistItem> items) {
        List<ChecklistItemResponseDTO> itemDTOs = items.stream()
                .map(LabelMapper::toResponseDTO)
                .toList();
        int total = items.size();
        int completed = (int) items.stream().filter(ChecklistItem::getIsCompleted).count();

        return ChecklistResponseDTO.builder()
                .checklistId(checklist.getChecklistId())
                .cardId(checklist.getCardId())
                .title(checklist.getTitle())
                .position(checklist.getPosition())
                .createdAt(checklist.getCreatedAt())
                .items(itemDTOs)
                .totalItems(total)
                .completedItems(completed)
                .build();
    }
}

