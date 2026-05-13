package com.flowboard.comment.mapper;

import com.flowboard.comment.dto.AttachmentResponseDTO;
import com.flowboard.comment.dto.CommentResponseDTO;
import com.flowboard.comment.dto.UserResponseDTO;
import com.flowboard.comment.entity.Attachment;
import com.flowboard.comment.entity.Comment;

public class CommentMapper {
    private CommentMapper() {}

    public static CommentResponseDTO toResponseDTO(Comment comment, int replyCount) {
        return toResponseDTO(comment, replyCount, null);
    }

    public static CommentResponseDTO toResponseDTO(Comment comment) {
        return toResponseDTO(comment, 0, null);
    }

    // Enriched mapper variant — accepts the resolved author so callers can attach
    // email/full-name without forcing a Feign lookup at every call site.
    public static CommentResponseDTO toResponseDTO(Comment comment, int replyCount, UserResponseDTO author) {
        CommentResponseDTO.CommentResponseDTOBuilder builder = CommentResponseDTO.builder()
                .commentId(comment.getCommentId())
                .cardId(comment.getCardId())
                .authorId(comment.getAuthorId())
                .content(comment.getIsDeleted() ? "[deleted]" : comment.getContent())
                .parentCommentId(comment.getParentCommentId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .isDeleted(comment.getIsDeleted())
                .replyCount(replyCount);

        if (author != null) {
            builder.authorEmail(author.getEmail())
                   .authorName(author.getFullName());
        }
        return builder.build();
    }

    public static AttachmentResponseDTO toResponseDTO(Attachment attachment) {
        return AttachmentResponseDTO.builder()
                .attachmentId(attachment.getAttachmentId())
                .cardId(attachment.getCardId())
                .uploaderId(attachment.getUploaderId())
                .fileName(attachment.getFileName())
                .fileUrl(attachment.getFileUrl())
                .fileType(attachment.getFileType())
                .sizeKb(attachment.getSizeKb())
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }
}
