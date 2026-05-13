package com.flowboard.card.repository;

import com.flowboard.card.entity.Card;
import com.flowboard.card.enums.Priority;
import com.flowboard.card.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    // Find by list - ordered by position (primary use: board rendering)
    List<Card> findByListIdAndIsArchivedFalseOrderByPosition(Long listId);

    // For internal use (card-service moveCard needs all cards in a list)
    List<Card> findByListIdOrderByPosition(Long listId);

    // All active cards on a board
    List<Card> findByBoardIdAndIsArchivedFalse(Long boardId);

    // All cards on a board (including archived)
    List<Card> findByBoardId(Long boardId);

    // Cards assigned to a specific user (active only)
    List<Card> findByAssigneeIdAndIsArchivedFalse(Long assigneeId);

    // Overdue: due date passed and status != DONE
    @Query("SELECT c FROM Card c WHERE c.dueDate < :today AND c.status != 'DONE' AND c.isArchived = false")
    List<Card> findOverdueCards(@Param("today") LocalDate today);

    // Overdue on a specific board
    @Query("SELECT c FROM Card c WHERE c.boardId = :boardId AND c.dueDate < :today AND c.status != 'DONE' AND c.isArchived = false")
    List<Card> findOverdueCardsByBoard(@Param("boardId") Long boardId, @Param("today") LocalDate today);

    // Filter by priority on a board
    List<Card> findByBoardIdAndPriorityAndIsArchivedFalse(Long boardId, Priority priority);

    // Filter by status on a board
    List<Card> findByBoardIdAndStatusAndIsArchivedFalse(Long boardId, Status status);

    // Archived cards
    List<Card> findByBoardIdAndIsArchivedTrue(Long boardId);
    List<Card> findByListIdAndIsArchivedTrue(Long listId);

    // Count cards per list (for analytics)
    long countByListIdAndIsArchivedFalse(Long listId);
    long countByBoardIdAndIsArchivedFalse(Long boardId);

    // Max position in a list (for new card placement)
    @Query("SELECT MAX(c.position) FROM Card c WHERE c.listId = :listId AND c.isArchived = false")
    Optional<Integer> findMaxPositionByListId(@Param("listId") Long listId);

    // Single card lookup
    Optional<Card> findByCardId(Long cardId);
}
