package com.flowboard.card.service;

import com.flowboard.card.dto.*;
import com.flowboard.card.entity.Card;
import com.flowboard.card.enums.Priority;
import com.flowboard.card.enums.Status;

import java.util.List;

public interface CardService {

    // CRUD
    Card createCard(CardRequestDTO dto, String userEmail);

    Card getCardById(Long cardId);

    List<Card> getCardsByList(Long listId);

    List<Card> getCardsByBoard(Long boardId);

    List<Card> getCardsByAssignee(Long assigneeId);

    Card updateCard(Long cardId, CardRequestDTO dto, String userEmail);

    void deleteCard(Long cardId, String userEmail);

    // Move / Reorder
    Card moveCard(Long cardId, MoveCardRequestDTO dto, String userEmail);

    List<Card> reorderCards(Long listId, ReorderCardsRequestDTO dto, String userEmail);

    // Archival
    Card archiveCard(Long cardId, String userEmail);

    Card unarchiveCard(Long cardId, String userEmail);

    List<Card> getArchivedCardsByBoard(Long boardId);

    List<Card> getArchivedCardsByList(Long listId);

    // Assignment & Priority
    Card setAssignee(Long cardId, AssigneeRequestDTO dto, String userEmail);

    Card setPriority(Long cardId, PriorityRequestDTO dto, String userEmail);

    Card setStatus(Long cardId, StatusRequestDTO dto, String userEmail);

    // Overdue Detection
    List<Card> getOverdueCards();

    List<Card> getOverdueCardsByBoard(Long boardId);

    // Filtering
    List<Card> getCardsByBoardAndPriority(Long boardId, Priority priority);

    List<Card> getCardsByBoardAndStatus(Long boardId, Status status);
}
