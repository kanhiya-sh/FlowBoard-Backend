package com.flowboard.label;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowboard.label.controller.ChecklistController;
import com.flowboard.label.dto.*;
import com.flowboard.label.service.LabelService;
import com.flowboard.label.util.JwtUtil;
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

@WebMvcTest(controllers = ChecklistController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChecklistControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private LabelService labelService;
    @MockBean private JwtUtil jwtUtil;

    private ChecklistResponseDTO mockChecklist(Long id) {
        return ChecklistResponseDTO.builder()
                .checklistId(id).cardId(10L).title("List").position(0)
                .items(List.of()).build();
    }

    private ChecklistItemResponseDTO mockItem(Long id) {
        return ChecklistItemResponseDTO.builder()
                .itemId(id).checklistId(1L).text("item").isCompleted(false).build();
    }

    @Test
    void createChecklist_returnsCreated() throws Exception {
        ChecklistRequestDTO req = new ChecklistRequestDTO();
        req.setCardId(10L); req.setTitle("List");

        when(labelService.createChecklist(any(), eq("alice@test.com"))).thenReturn(mockChecklist(1L));

        mockMvc.perform(post("/checklists")
                        .requestAttr("userEmail", "alice@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checklistId").value(1));
    }

    @Test
    void createChecklist_invalidBody_returns400() throws Exception {
        ChecklistRequestDTO req = new ChecklistRequestDTO();
        mockMvc.perform(post("/checklists")
                        .requestAttr("userEmail", "alice@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getChecklistsByCard_returnsList() throws Exception {
        when(labelService.getChecklistsByCard(10L))
                .thenReturn(List.of(mockChecklist(1L), mockChecklist(2L)));

        mockMvc.perform(get("/checklists/card/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void deleteChecklist_returnsOk() throws Exception {
        doNothing().when(labelService).deleteChecklist(1L, "alice@test.com");

        mockMvc.perform(delete("/checklists/1").requestAttr("userEmail", "alice@test.com"))
                .andExpect(status().isOk());
    }

    @Test
    void getChecklistProgress_returnsDto() throws Exception {
        ChecklistProgressDTO progress = ChecklistProgressDTO.builder()
                .checklistId(1L).title("X").totalItems(4).completedItems(2).completionPercentage(50.0).build();
        when(labelService.getChecklistProgress(1L)).thenReturn(progress);

        mockMvc.perform(get("/checklists/1/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionPercentage").value(50.0));
    }

    @Test
    void addItem_returnsCreated() throws Exception {
        ChecklistItemRequestDTO req = new ChecklistItemRequestDTO();
        req.setChecklistId(1L); req.setText("New item");

        when(labelService.addItem(any(), eq("alice@test.com"))).thenReturn(mockItem(7L));

        mockMvc.perform(post("/checklists/items")
                        .requestAttr("userEmail", "alice@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(7));
    }

    @Test
    void addItem_invalidBody_returns400() throws Exception {
        ChecklistItemRequestDTO req = new ChecklistItemRequestDTO();
        mockMvc.perform(post("/checklists/items")
                        .requestAttr("userEmail", "alice@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void toggleItem_returnsUpdated() throws Exception {
        when(labelService.toggleItem(7L, "alice@test.com")).thenReturn(mockItem(7L));

        mockMvc.perform(put("/checklists/items/7/toggle").requestAttr("userEmail", "alice@test.com"))
                .andExpect(status().isOk());
    }

    @Test
    void updateItem_returnsUpdated() throws Exception {
        ChecklistItemRequestDTO req = new ChecklistItemRequestDTO();
        req.setChecklistId(1L); req.setText("edit");

        when(labelService.updateItem(eq(7L), any(), eq("alice@test.com"))).thenReturn(mockItem(7L));

        mockMvc.perform(put("/checklists/items/7")
                        .requestAttr("userEmail", "alice@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteItem_returnsOk() throws Exception {
        doNothing().when(labelService).deleteItem(7L, "alice@test.com");

        mockMvc.perform(delete("/checklists/items/7").requestAttr("userEmail", "alice@test.com"))
                .andExpect(status().isOk());
    }

    @Test
    void getChecklistsByCardInternal_returnsList() throws Exception {
        when(labelService.getChecklistsByCard(10L)).thenReturn(List.of(mockChecklist(1L)));

        mockMvc.perform(get("/checklists/internal/card/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
