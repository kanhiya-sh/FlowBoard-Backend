package com.flowboard.board;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowboard.board.controller.BoardController;
import com.flowboard.board.dto.*;
import com.flowboard.board.entity.Board;
import com.flowboard.board.enums.BoardRole;
import com.flowboard.board.enums.Visibility;
import com.flowboard.board.service.BoardService;
import com.flowboard.board.util.JwtUtil;
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

@WebMvcTest(controllers = BoardController.class)
@AutoConfigureMockMvc(addFilters = false)
class BoardControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private BoardService boardService;
    @MockBean private JwtUtil jwtUtil;

    private Board mockBoard(Long id) {
        return Board.builder().boardId(id).name("B").workspaceId(1L)
                .visibility(Visibility.PRIVATE).isClosed(false).build();
    }

    @Test
    void createBoard_returnsOk() throws Exception {
        BoardRequestDTO req = new BoardRequestDTO();
        req.setName("B"); req.setWorkspaceId(1L); req.setVisibility(Visibility.PRIVATE);
        when(boardService.createBoard(any(), eq("u@e.com"))).thenReturn(mockBoard(5L));

        mockMvc.perform(post("/boards")
                        .requestAttr("userEmail", "u@e.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boardId").value(5));
    }

    @Test
    void createBoard_invalidBody_returns400() throws Exception {
        BoardRequestDTO req = new BoardRequestDTO();
        mockMvc.perform(post("/boards")
                        .requestAttr("userEmail", "u@e.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBoardById_returnsOk() throws Exception {
        when(boardService.getBoardById(5L)).thenReturn(mockBoard(5L));
        mockMvc.perform(get("/boards/5")).andExpect(status().isOk())
                .andExpect(jsonPath("$.boardId").value(5));
    }

    @Test
    void getBoardsByWorkspace_returnsList() throws Exception {
        when(boardService.getBoardsByWorkspace(1L)).thenReturn(List.of(mockBoard(1L)));
        mockMvc.perform(get("/boards/workspace/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getBoardsByUser_returnsList() throws Exception {
        when(boardService.getBoardsByUser(1L)).thenReturn(List.of(mockBoard(1L)));
        mockMvc.perform(get("/boards/user/1"))
                .andExpect(status().isOk());
    }

    @Test
    void updateBoard_returnsOk() throws Exception {
        BoardRequestDTO req = new BoardRequestDTO();
        req.setName("B"); req.setWorkspaceId(1L); req.setVisibility(Visibility.PRIVATE);
        when(boardService.updateBoard(eq(5L), any(), eq("u@e.com"))).thenReturn(mockBoard(5L));

        mockMvc.perform(put("/boards/5")
                        .requestAttr("userEmail", "u@e.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteBoard_returnsOk() throws Exception {
        doNothing().when(boardService).deleteBoard(5L, "u@e.com");
        mockMvc.perform(delete("/boards/5").requestAttr("userEmail", "u@e.com"))
                .andExpect(status().isOk());
    }

    @Test
    void closeBoard_returnsOk() throws Exception {
        when(boardService.closeBoard(5L, "u@e.com")).thenReturn(mockBoard(5L));
        mockMvc.perform(put("/boards/5/close").requestAttr("userEmail", "u@e.com"))
                .andExpect(status().isOk());
    }

    @Test
    void addMember_returnsOk() throws Exception {
        when(boardService.addMember(eq(1L), eq(5L), eq(BoardRole.MEMBER), eq("u@e.com")))
                .thenReturn(BoardMemberResponseDTO.builder().userId(5L).role(BoardRole.MEMBER).build());

        mockMvc.perform(post("/boards/1/members")
                        .requestAttr("userEmail", "u@e.com")
                        .param("userId", "5").param("role", "MEMBER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(5));
    }

    @Test
    void removeMember_returnsOk() throws Exception {
        doNothing().when(boardService).removeMember(1L, 5L, "u@e.com");
        mockMvc.perform(delete("/boards/1/members/5").requestAttr("userEmail", "u@e.com"))
                .andExpect(status().isOk());
    }

    @Test
    void updateMemberRole_returnsOk() throws Exception {
        when(boardService.updateMemberRole(eq(1L), eq(5L), eq(BoardRole.ADMIN), eq("u@e.com")))
                .thenReturn(BoardMemberResponseDTO.builder().userId(5L).role(BoardRole.ADMIN).build());

        mockMvc.perform(put("/boards/1/members/5/role")
                        .requestAttr("userEmail", "u@e.com")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void getMembers_returnsList() throws Exception {
        when(boardService.getBoardMembers(1L))
                .thenReturn(List.of(BoardMemberResponseDTO.builder().userId(5L).role(BoardRole.MEMBER).build()));
        mockMvc.perform(get("/boards/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAssignableUsers_returnsList() throws Exception {
        when(boardService.getAssignableUsers(1L))
                .thenReturn(List.of(BoardMemberResponseDTO.builder().userId(5L).role(BoardRole.MEMBER).build()));
        mockMvc.perform(get("/boards/1/assignable-users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void checkBoardMembership_returnsResult() throws Exception {
        when(boardService.checkBoardMembership(1L, 5L))
                .thenReturn(new BoardMemberCheckResponseDTO(true, "MEMBER"));
        mockMvc.perform(get("/boards/internal/1/members/5/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMember").value(true))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }
}
