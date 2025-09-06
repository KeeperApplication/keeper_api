package xyz.piod.keeper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationPayload {
    private String recipientUsername;
    private String senderUsername;
    private String messageContent;
    private Long roomId;
    private String fcmToken;
}