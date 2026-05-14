package com.flowboard.label;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowboard.label.controller.LabelController;
import com.flowboard.label.dto.LabelRequestDTO;
import com.flowboard.label.dto.LabelResponseDTO;
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

@WebMvcTest(controllers = LabelController.class)
@AutoConfigureMockMvc(addFilters = false)
class LabelControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private LabelService labelService;
    @MockBean private JwtUtil jwtUtil;

    private LabelResponseDTO mockLabel(Long id) {
        return LabelResponseDTO.builder()
                .labelId(id).boardId(1L).name("Bug").color("#FF0000").build();
    }

    @Test
    void createLabel_returnsCreated() throws Exception {
        LabelRequestDTO req = new LabelRequestDTO();
        req.setBoardId(1L); req.setName("Bug"); req.setColor("#FF0000");

        when(labelService.createLabel(any(), eq("alice@test.com"))).thenReturn(mockLabel(5L));

        mockMvc.perform(post("/labels")
                        .requestAttr("userEmail", "alice@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labelId").value(5));
    }

    @Test
    void createLabel_invalidBody_returns400() throws Exception {
        LabelRequestDTO req = new LabelRequestDTO();
        mockMvc.perform(post("/labels")
                        .requestAttr("userEmail", "alice@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getLabelsByBoard_returnsList() throws Exception {
        when(labelService.getLabelsByBoard(1L)).thenReturn(List.of(mockLabel(1L), mockLabel(2L)));

        mockMvc.perform(get("/labels/board/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void updateLabel_returnsUpdated() throws Exception {
        LabelRequestDTO req = new LabelRequestDTO();
        req.setBoardId(1L); req.setName("Renamed"); req.setColor("#00FF00");

        when(labelService.updateLabel(eq(5L), any(), eq("alice@test.com"))).thenReturn(mockLabel(5L));

        mockMvc.perform(put("/labels/5")
                        .requestAttr("userEmail", "alice@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteLabel_returnsOk() throws Exception {
        doNothing().when(labelService).deleteLabel(5L, "alice@test.com");

        mockMvc.perform(delete("/labels/5").requestAttr("userEmail", "alice@test.com"))
                .andExpect(status().isOk());

        verify(labelService).deleteLabel(5L, "alice@test.com");
    }

    @Test
    void addLabelToCard_returnsOk() throws Exception {
        doNothing().when(labelService).addLabelToCard(10L, 5L, "alice@test.com");

        mockMvc.perform(post("/labels/5/card/10").requestAttr("userEmail", "alice@test.com"))
                .andExpect(status().isOk());

        verify(labelService).addLabelToCard(10L, 5L, "alice@test.com");
    }

    @Test
    void removeLabelFromCard_returnsOk() throws Exception {
        doNothing().when(labelService).removeLabelFromCard(10L, 5L, "alice@test.com");

        mockMvc.perform(delete("/labels/5/card/10").requestAttr("userEmail", "alice@test.com"))
                .andExpect(status().isOk());
    }

    @Test
    void getLabelsForCard_returnsList() throws Exception {
        when(labelService.getLabelsForCard(10L)).thenReturn(List.of(mockLabel(1L)));

        mockMvc.perform(get("/labels/card/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getLabelsForCardInternal_returnsList() throws Exception {
        when(labelService.getLabelsForCard(10L)).thenReturn(List.of(mockLabel(1L), mockLabel(2L)));

        mockMvc.perform(get("/labels/internal/card/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
