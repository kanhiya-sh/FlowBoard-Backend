package com.flowboard.label.repository;

import com.flowboard.label.entity.CardLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface CardLabelRepository extends JpaRepository<CardLabel, Long> {
    List<CardLabel> findByCardId(Long cardId);
    List<CardLabel> findByLabelId(Long labelId);
    Optional<CardLabel> findByCardIdAndLabelId(Long cardId, Long labelId);
    boolean existsByCardIdAndLabelId(Long cardId, Long labelId);
    void deleteByLabelId(Long labelId);
}

