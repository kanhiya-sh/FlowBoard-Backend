package com.flowboard.list.service;

import com.flowboard.list.dto.ListRequestDTO;
import com.flowboard.list.dto.MoveListRequestDTO;
import com.flowboard.list.dto.ReorderRequestDTO;
import com.flowboard.list.entity.TaskList;
import java.util.List;

public interface ListService {
    // ---- CRUD ----
    TaskList createList(ListRequestDTO dto, String userEmail);

    TaskList getListById(Long listId);

    List<TaskList> getListsByBoard(Long boardId);

    List<TaskList> getListsByBoardOrdered(Long boardId);

    TaskList updateList(Long listId, ListRequestDTO dto, String userEmail);

    void deleteList(Long listId, String userEmail);

    // ---- Position Management ----
    List<TaskList> reorderLists(Long boardId, ReorderRequestDTO dto, String userEmail);

    // ---- Archival ----
    TaskList archiveList(Long listId, String userEmail);

    TaskList unarchiveList(Long listId, String userEmail);

    List<TaskList> getArchivedLists(Long boardId);

    // ---- Board Transfer ----
    TaskList moveList(Long listId, MoveListRequestDTO dto, String userEmail);
}
