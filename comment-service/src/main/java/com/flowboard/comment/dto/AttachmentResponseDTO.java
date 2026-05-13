package com.flowboard.comment.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AttachmentResponseDTO {
    private Long attachmentId;
    private Long cardId;
    private Long uploaderId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long sizeKb;
    private LocalDateTime uploadedAt;
}
