package com.flowboard.comment.service;

import com.flowboard.comment.dto.AttachmentRequestDTO;
import com.flowboard.comment.dto.AttachmentResponseDTO;
import com.flowboard.comment.dto.CommentRequestDTO;
import com.flowboard.comment.dto.CommentResponseDTO;
import com.flowboard.comment.dto.CommentUpdateDTO;
import java.util.List;

public interface CommentService {

    // Comment CRUD
    CommentResponseDTO addComment(CommentRequestDTO dto, String userEmail);
    List<CommentResponseDTO> getByCard(Long cardId);
    CommentResponseDTO getCommentById(Long commentId);
    List<CommentResponseDTO> getReplies(Long parentCommentId);
    CommentResponseDTO updateComment(Long commentId, CommentUpdateDTO dto, String userEmail);
    void deleteComment(Long commentId, String userEmail);
    long getCommentCount(Long cardId);

    // Attachment CRUD
    AttachmentResponseDTO addAttachment(AttachmentRequestDTO dto, String userEmail);
    List<AttachmentResponseDTO> getAttachmentsByCard(Long cardId);
    void deleteAttachment(Long attachmentId, String userEmail);
}
