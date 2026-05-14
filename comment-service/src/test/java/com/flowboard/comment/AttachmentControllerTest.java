package com.flowboard.comment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowboard.comment.controller.AttachmentController;
import com.flowboard.comment.dto.*;
import com.flowboard.comment.service.CommentService;
import com.flowboard.comment.util.JwtUtil;
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

@WebMvcTest(controllers = AttachmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class AttachmentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CommentService commentService;
    @MockBean private JwtUtil jwtUtil;

    private AttachmentResponseDTO mockResp(Long id) {
        return AttachmentResponseDTO.builder()
                .attachmentId(id)
                .cardId(1L)
                .fileName("a.txt")
                .build();
    }

    @Test
    void addAttachment_returnsOk() throws Exception {
        AttachmentRequestDTO req = new AttachmentRequestDTO();
        req.setCardId(1L); req.setFileName("a.txt");
        req.setFileUrl("http://x"); req.setFileType("txt"); req.setSizeKb(10L);

        when(commentService.addAttachment(any(), eq("u@e.com"))).thenReturn(mockResp(5L));

        mockMvc.perform(post("/attachments")
                        .requestAttr("userEmail", "u@e.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attachmentId").value(5));
    }

    @Test
    void addAttachment_invalidBody_returns400() throws Exception {
        AttachmentRequestDTO req = new AttachmentRequestDTO();
        mockMvc.perform(post("/attachments")
                        .requestAttr("userEmail", "u@e.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAttachmentsByCard_returnsList() throws Exception {
        when(commentService.getAttachmentsByCard(1L))
                .thenReturn(List.of(mockResp(1L), mockResp(2L)));

        mockMvc.perform(get("/attachments/card/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void deleteAttachment_returnsOk() throws Exception {
        doNothing().when(commentService).deleteAttachment(5L, "u@e.com");

        mockMvc.perform(delete("/attachments/5").requestAttr("userEmail", "u@e.com"))
                .andExpect(status().isOk());
    }
}
