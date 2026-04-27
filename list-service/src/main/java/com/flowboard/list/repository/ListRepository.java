package com.flowboard.list.repository;

import com.flowboard.list.entity.TaskList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ListRepository extends JpaRepository<TaskList, Long> {

    List<TaskList> findByBoardId(Long boardId);

    Optional<TaskList> findByListId(Long listId);

    List<TaskList> findByBoardIdOrderByPosition(Long boardId);

    List<TaskList> findByBoardIdAndIsArchived(Long boardId, Boolean isArchived);

    long countByBoardId(Long boardId);

    @Query("SELECT MAX(t.position) FROM TaskList t WHERE t.boardId = :boardId AND t.isArchived = false")
    Optional<Integer> findMaxPositionByBoardId(@Param("boardId") Long boardId);

    void deleteByListId(Long listId);
}
