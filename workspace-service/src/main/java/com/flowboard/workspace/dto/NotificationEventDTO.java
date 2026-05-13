package com.flowboard.workspace.dto;

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
    private String type;
    private String message;
    private String title;
    private Long relatedId;
    private String relatedType;
    private String deepLinkUrl;
}
