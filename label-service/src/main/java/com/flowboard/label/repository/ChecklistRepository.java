package com.flowboard.label.repository;

import com.flowboard.label.entity.Checklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ChecklistRepository extends JpaRepository<Checklist, Long> {
    List<Checklist> findByCardIdOrderByPositionAsc(Long cardId);
    long countByCardId(Long cardId);

    @Query("SELECT MAX(c.position) FROM Checklist c WHERE c.cardId = :cardId")
    Optional<Integer> findMaxPositionByCardId(@Param("cardId") Long cardId);
}

