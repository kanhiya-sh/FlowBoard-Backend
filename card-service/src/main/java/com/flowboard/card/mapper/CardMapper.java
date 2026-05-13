package com.flowboard.card.mapper;

import com.flowboard.card.dto.CardResponseDTO;
import com.flowboard.card.entity.Card;
import com.flowboard.card.enums.Status;
import java.time.LocalDate;

public class CardMapper {

    private CardMapper() {}

    public static CardResponseDTO toResponseDTO(Card card) {
        boolean overdue = card.getDueDate() != null
                && card.getDueDate().isBefore(LocalDate.now())
                && card.getStatus() != Status.DONE;

        return CardResponseDTO.builder()
                .cardId(card.getCardId())
                .listId(card.getListId())
                .boardId(card.getBoardId())
                .title(card.getTitle())
                .description(card.getDescription())
                .position(card.getPosition())
                .priority(card.getPriority())
                .status(card.getStatus())
                .dueDate(card.getDueDate())
                .startDate(card.getStartDate())
                .assigneeId(card.getAssigneeId())
                .createdById(card.getCreatedById())
                .isArchived(card.getIsArchived())
                .coverColor(card.getCoverColor())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .isOverdue(overdue)
                .build();
    }
}
