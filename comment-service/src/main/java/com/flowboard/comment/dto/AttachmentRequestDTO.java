package com.flowboard.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttachmentRequestDTO {

    @NotNull(message = "cardId is required")
    private Long cardId;

    @NotBlank(message = "fileName is required")
    private String fileName;

    @NotBlank(message = "fileUrl is required")
    private String fileUrl;

    @NotBlank(message = "fileType is required")
    private String fileType;

    @NotNull(message = "sizeKb is required")
    private Long sizeKb;
}
