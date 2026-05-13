package com.flowboard.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEventDTO {
    private Long actorId;
    private Long recipientId;
    private String type;           // COMMENT / MENTION / ATTACHMENT / ASSIGNMENT / MOVE / DUE_DATE
    private String message;
    private String title;
    private Long relatedId;        // cardId or boardId
    private String relatedType;    // "CARD", "BOARD"
    private String deepLinkUrl;
}

