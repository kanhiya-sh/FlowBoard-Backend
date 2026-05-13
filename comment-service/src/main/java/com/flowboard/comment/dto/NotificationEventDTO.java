package com.flowboard.comment.dto;

import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class NotificationEventDTO {
    private Long actorId;
    private Long recipientId;
    private String type;           // COMMENT / MENTION / ATTACHMENT
    private String message;
    private String title;
    private Long relatedId;        // cardId
    private String relatedType;    // "CARD"
    private String deepLinkUrl;
}
