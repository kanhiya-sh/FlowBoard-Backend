package com.flowboard.comment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowboard.comment.controller.CommentController;
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

@WebMvcTest(controllers = CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CommentService commentService;
    @MockBean private JwtUtil jwtUtil;

    private CommentResponseDTO mockResp(Long id) {
        return CommentResponseDTO.builder()
                .commentId(id)
                .cardId(1L)
                .content("Hi")
                .build();
    }

    @Test
    void addComment_returnsOk() throws Exception {
        CommentRequestDTO req = new CommentRequestDTO();
        req.setCardId(1L); req.setContent("Hi");

        when(commentService.addComment(any(), eq("u@e.com"))).thenReturn(mockResp(5L));

        mockMvc.perform(post("/comments")
                        .requestAttr("userEmail", "u@e.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(5));
    }

    @Test
    void addComment_invalidBody_returns400() throws Exception {
        CommentRequestDTO req = new CommentRequestDTO();
        mockMvc.perform(post("/comments")
                        .requestAttr("userEmail", "u@e.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByCard_returnsList() throws Exception {
        when(commentService.getByCard(1L)).thenReturn(List.of(mockResp(1L), mockResp(2L)));

        mockMvc.perform(get("/comments/card/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getCommentCount_returnsValue() throws Exception {
        when(commentService.getCommentCount(1L)).thenReturn(7L);

        mockMvc.perform(get("/comments/card/1/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));
    }

    @Test
    void getCommentById_returnsComment() throws Exception {
        when(commentService.getCommentById(5L)).thenReturn(mockResp(5L));

        mockMvc.perform(get("/comments/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(5));
    }

    @Test
    void getReplies_returnsList() throws Exception {
        when(commentService.getReplies(5L)).thenReturn(List.of(mockResp(6L)));

        mockMvc.perform(get("/comments/5/replies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void updateComment_returnsUpdated() throws Exception {
        CommentUpdateDTO dto = new CommentUpdateDTO();
        dto.setContent("Updated");

        when(commentService.updateComment(eq(5L), any(), eq("u@e.com"))).thenReturn(mockResp(5L));

        mockMvc.perform(put("/comments/5")
                        .requestAttr("userEmail", "u@e.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void updateComment_invalidBody_returns400() throws Exception {
        CommentUpdateDTO dto = new CommentUpdateDTO();
        mockMvc.perform(put("/comments/5")
                        .requestAttr("userEmail", "u@e.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteComment_returnsOk() throws Exception {
        doNothing().when(commentService).deleteComment(5L, "u@e.com");

        mockMvc.perform(delete("/comments/5").requestAttr("userEmail", "u@e.com"))
                .andExpect(status().isOk());
    }

    @Test
    void getCommentCountInternal_returnsValue() throws Exception {
        when(commentService.getCommentCount(1L)).thenReturn(3L);

        mockMvc.perform(get("/comments/internal/card/1/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }
}
