package com.flowboard.card;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowboard.card.controller.CardController;
import com.flowboard.card.dto.*;
import com.flowboard.card.entity.Card;
import com.flowboard.card.enums.Priority;
import com.flowboard.card.enums.Status;
import com.flowboard.card.service.CardService;
import com.flowboard.card.util.JwtUtil;
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

@WebMvcTest(controllers = CardController.class)
@AutoConfigureMockMvc(addFilters = false)
class CardControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CardService cardService;
    @MockBean private JwtUtil jwtUtil;

    private Card mockCard(Long id, boolean archived) {
        return Card.builder()
                .cardId(id).listId(1L).boardId(1L).title("T")
                .priority(Priority.MEDIUM).status(Status.TO_DO)
                .position(1).isArchived(archived).build();
    }

    @Test
    void createCard_returnsOk() throws Exception {
        CardRequestDTO req = new CardRequestDTO();
        req.setTitle("T"); req.setListId(1L); req.setBoardId(1L);
        when(cardService.createCard(any(), eq("u@e.com"))).thenReturn(mockCard(5L, false));

        mockMvc.perform(post("/cards")
                        .requestAttr("userEmail", "u@e.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardId").value(5));
    }

    @Test
    void createCard_invalidBody_returns400() throws Exception {
        CardRequestDTO req = new CardRequestDTO();
        mockMvc.perform(post("/cards")
                        .requestAttr("userEmail", "u@e.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCardById_returnsCard() throws Exception {
        when(cardService.getCardById(5L)).thenReturn(mockCard(5L, false));

        mockMvc.perform(get("/cards/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardId").value(5));
    }

    @Test
    void getCardsByList_returnsList() throws Exception {
        when(cardService.getCardsByList(1L)).thenReturn(List.of(mockCard(1L, false)));

        mockMvc.perform(get("/cards/list/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getCardsByBoard_returnsList() throws Exception {
        when(cardService.getCardsByBoard(1L)).thenReturn(List.of(mockCard(1L, false)));

        mockMvc.perform(get("/cards/board/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getCardsByAssignee_returnsList() throws Exception {
        when(cardService.getCardsByAssignee(5L)).thenReturn(List.of(mockCard(1L, false)));

        mockMvc.perform(get("/cards/assignee/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void updateCard_returnsUpdated() throws Exception {
        CardRequestDTO req = new CardRequestDTO();
        req.setTitle("T"); req.setListId(1L); req.setBoardId(1L);
        when(cardService.updateCard(eq(5L), any(), eq("u@e.com"))).thenReturn(mockCard(5L, false));

        mockMvc.perform(put("/cards/5")
                        .requestAttr("userEmail", "u@e.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteCard_returnsOk() throws Exception {
        doNothing().when(cardService).deleteCard(5L, "u@e.com");

        mockMvc.perform(delete("/cards/5").requestAttr("userEmail", "u@e.com"))
                .andExpect(status().isOk());
    }

    @Test
    void moveCard_returnsCard() throws Exception {
        MoveCardRequestDTO req = new MoveCardRequestDTO();
        req.setTargetListId(2L);
        when(cardService.moveCard(eq(5L), any(), eq("u@e.com"))).thenReturn(mockCard(5L, false));

        mockMvc.perform(put("/cards/5/move")
                        .requestAttr("userEmail", "u@e.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void moveCard_invalidBody_returns400() throws Exception {
        MoveCardRequestDTO req = new MoveCardRequestDTO();
        mockMvc.perform(put("/cards/5/move")
                        .requestAttr("userEmail", "u@e.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reorderCards_returnsList() throws Exception {
        ReorderCardsRequestDTO req = new ReorderCardsRequestDTO();
        req.setOrderedCardIds(List.of(1L, 2L));
        when(cardService.reorderCards(eq(1L), any(), eq("u@e.com")))
                .thenReturn(List.of(mockCard(1L, false), mockCard(2L, false)));

        mockMvc.perform(put("/cards/list/1/reorder")
                        .requestAttr("userEmail", "u@e.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void archiveCard_returnsArchived() throws Exception {
        when(cardService.archiveCard(5L, "u@e.com")).thenReturn(mockCard(5L, true));

        mockMvc.perform(post("/cards/5/archive").requestAttr("userEmail", "u@e.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isArchived").value(true));
    }

    @Test
    void unarchiveCard_returnsUnarchived() throws Exception {
        when(cardService.unarchiveCard(5L, "u@e.com")).thenReturn(mockCard(5L, false));

        mockMvc.perform(post("/cards/5/unarchive").requestAttr("userEmail", "u@e.com"))
                .andExpect(status().isOk());
    }

    @Test
    void getArchivedCardsByBoard_returnsList() throws Exception {
        when(cardService.getArchivedCardsByBoard(1L)).thenReturn(List.of(mockCard(1L, true)));

        mockMvc.perform(get("/cards/board/1/archived"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getArchivedCardsByList_returnsList() throws Exception {
        when(cardService.getArchivedCardsByList(1L)).thenReturn(List.of(mockCard(1L, true)));

        mockMvc.perform(get("/cards/list/1/archived"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void setAssignee_returnsCard() throws Exception {
        AssigneeRequestDTO req = new AssigneeRequestDTO();
        req.setAssigneeId(5L);
        when(cardService.setAssignee(eq(1L), any(), eq("u@e.com"))).thenReturn(mockCard(1L, false));

        mockMvc.perform(put("/cards/1/assignee")
                        .requestAttr("userEmail", "u@e.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void setPriority_returnsCard() throws Exception {
        PriorityRequestDTO req = new PriorityRequestDTO();
        req.setPriority(Priority.HIGH);
        when(cardService.setPriority(eq(1L), any(), eq("u@e.com"))).thenReturn(mockCard(1L, false));

        mockMvc.perform(put("/cards/1/priority")
                        .requestAttr("userEmail", "u@e.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void setStatus_returnsCard() throws Exception {
        StatusRequestDTO req = new StatusRequestDTO();
        req.setStatus(Status.IN_PROGRESS);
        when(cardService.setStatus(eq(1L), any(), eq("u@e.com"))).thenReturn(mockCard(1L, false));

        mockMvc.perform(put("/cards/1/status")
                        .requestAttr("userEmail", "u@e.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void getOverdueCards_returnsList() throws Exception {
        when(cardService.getOverdueCards()).thenReturn(List.of(mockCard(1L, false)));

        mockMvc.perform(get("/cards/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getOverdueCardsByBoard_returnsList() throws Exception {
        when(cardService.getOverdueCardsByBoard(1L)).thenReturn(List.of(mockCard(1L, false)));

        mockMvc.perform(get("/cards/board/1/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void filterCards_byPriority_returnsList() throws Exception {
        when(cardService.getCardsByBoardAndPriority(1L, Priority.HIGH))
                .thenReturn(List.of(mockCard(1L, false)));

        mockMvc.perform(get("/cards/board/1/filter").param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void filterCards_byStatus_returnsList() throws Exception {
        when(cardService.getCardsByBoardAndStatus(1L, Status.TO_DO))
                .thenReturn(List.of(mockCard(1L, false)));

        mockMvc.perform(get("/cards/board/1/filter").param("status", "TO_DO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void filterCards_noFilter_returnsBoard() throws Exception {
        when(cardService.getCardsByBoard(1L)).thenReturn(List.of(mockCard(1L, false)));

        mockMvc.perform(get("/cards/board/1/filter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void checkCardExists_returnsCard() throws Exception {
        when(cardService.getCardById(5L)).thenReturn(mockCard(5L, false));

        mockMvc.perform(get("/cards/internal/5/exists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardId").value(5));
    }

    @Test
    void getCardCountByBoard_returnsCount() throws Exception {
        when(cardService.getCardsByBoard(1L)).thenReturn(List.of(mockCard(1L, false), mockCard(2L, false)));

        mockMvc.perform(get("/cards/internal/board/1/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }
}
