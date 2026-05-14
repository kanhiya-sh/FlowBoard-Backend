package com.flowboard.list;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowboard.list.controller.ListController;
import com.flowboard.list.dto.*;
import com.flowboard.list.entity.TaskList;
import com.flowboard.list.service.ListService;
import com.flowboard.list.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ListController.class)
@AutoConfigureMockMvc(addFilters = false)
class ListControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ListService listService;
    @MockBean private JwtUtil jwtUtil;

    private TaskList mockList(Long id, boolean archived) {
        return TaskList.builder()
                .listId(id).boardId(1L).name("To Do").position(1)
                .isArchived(archived).build();
    }

    // ─── createList ───────────────────────────────────────────────────────────

    @Test
    void createList_returnsCreated() throws Exception {
        ListRequestDTO req = new ListRequestDTO();
        req.setName("To Do"); req.setBoardId(1L);

        when(listService.createList(any(), eq("alice@test.com"))).thenReturn(mockList(5L, false));

        mockMvc.perform(post("/lists")
                        .requestAttr("userEmail", "alice@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listId").value(5));
    }

    @Test
    void createList_invalidBody_returns400() throws Exception {
        ListRequestDTO req = new ListRequestDTO();
        mockMvc.perform(post("/lists")
                        .requestAttr("userEmail", "alice@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ─── getListById ──────────────────────────────────────────────────────────

    @Test
    void getListById_returnsList() throws Exception {
        when(listService.getListById(5L)).thenReturn(mockList(5L, false));
        doNothing().when(listService).validateBoardMembership(eq(1L), eq("alice@test.com"));

        mockMvc.perform(get("/lists/5").requestAttr("userEmail", "alice@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listId").value(5));
    }

    // ─── getListsByBoard / All / Archived ─────────────────────────────────────

    @Test
    void getListsByBoard_filtersArchived() throws Exception {
        when(listService.getListsByBoardOrdered(1L))
                .thenReturn(List.of(mockList(1L, false), mockList(2L, true)));
        doNothing().when(listService).validateBoardMembership(eq(1L), any());

        mockMvc.perform(get("/lists/board/1").requestAttr("userEmail", "alice@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAllListsByBoard_includesArchived() throws Exception {
        when(listService.getListsByBoardOrdered(1L))
                .thenReturn(List.of(mockList(1L, false), mockList(2L, true)));
        doNothing().when(listService).validateBoardMembership(eq(1L), any());

        mockMvc.perform(get("/lists/board/1/all").requestAttr("userEmail", "alice@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getArchivedLists_returnsList() throws Exception {
        when(listService.getArchivedLists(1L))
                .thenReturn(List.of(mockList(2L, true)));
        doNothing().when(listService).validateBoardMembership(eq(1L), any());

        mockMvc.perform(get("/lists/board/1/archived").requestAttr("userEmail", "alice@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ─── updateList ───────────────────────────────────────────────────────────

    @Test
    void updateList_returnsUpdated() throws Exception {
        ListRequestDTO req = new ListRequestDTO();
        req.setName("Renamed"); req.setBoardId(1L);

        when(listService.updateList(eq(5L), any(), eq("alice@test.com"))).thenReturn(mockList(5L, false));

        mockMvc.perform(put("/lists/5")
                        .requestAttr("userEmail", "alice@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // ─── deleteList ───────────────────────────────────────────────────────────

    @Test
    void deleteList_returnsOk() throws Exception {
        doNothing().when(listService).deleteList(5L, "alice@test.com");

        mockMvc.perform(delete("/lists/5").requestAttr("userEmail", "alice@test.com"))
                .andExpect(status().isOk());
    }

    // ─── reorderLists ─────────────────────────────────────────────────────────

    @Test
    void reorderLists_returnsList() throws Exception {
        ReorderRequestDTO req = new ReorderRequestDTO();
        req.setOrderedListIds(List.of(1L, 2L));

        when(listService.reorderLists(eq(1L), any(), eq("alice@test.com")))
                .thenReturn(List.of(mockList(1L, false), mockList(2L, false)));

        mockMvc.perform(put("/lists/board/1/reorder")
                        .requestAttr("userEmail", "alice@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ─── archive/unarchive ────────────────────────────────────────────────────

    @Test
    void archiveList_returnsArchived() throws Exception {
        when(listService.archiveList(5L, "alice@test.com")).thenReturn(mockList(5L, true));

        mockMvc.perform(post("/lists/5/archive").requestAttr("userEmail", "alice@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isArchived").value(true));
    }

    @Test
    void unarchiveList_returnsUnarchived() throws Exception {
        when(listService.unarchiveList(5L, "alice@test.com")).thenReturn(mockList(5L, false));

        mockMvc.perform(post("/lists/5/unarchive").requestAttr("userEmail", "alice@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isArchived").value(false));
    }

    // ─── moveList ─────────────────────────────────────────────────────────────

    @Test
    void moveList_returnsMoved() throws Exception {
        MoveListRequestDTO req = new MoveListRequestDTO();
        req.setTargetBoardId(2L);

        when(listService.moveList(eq(5L), any(), eq("alice@test.com")))
                .thenReturn(mockList(5L, false));

        mockMvc.perform(put("/lists/5/move")
                        .requestAttr("userEmail", "alice@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void moveList_invalidBody_returns400() throws Exception {
        MoveListRequestDTO req = new MoveListRequestDTO();

        mockMvc.perform(put("/lists/5/move")
                        .requestAttr("userEmail", "alice@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ─── Internal endpoints ───────────────────────────────────────────────────

    @Test
    void getListCountByBoard_returnsCount() throws Exception {
        when(listService.getListsByBoard(1L))
                .thenReturn(List.of(mockList(1L, false), mockList(2L, false)));

        mockMvc.perform(get("/lists/internal/board/1/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    @Test
    void checkListExists_returnsList() throws Exception {
        when(listService.getListById(5L)).thenReturn(mockList(5L, false));

        mockMvc.perform(get("/lists/internal/5/exists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listId").value(5));
    }
}
