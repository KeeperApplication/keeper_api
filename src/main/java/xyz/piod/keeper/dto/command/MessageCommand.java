package xyz.piod.keeper.dto.command;

import lombok.Data;

@Data
public class MessageCommand {
    private String userToken;
    private Long roomId;
    private Long messageId;
    private String content;
    private Long repliedToId;
    private String emoji;
    private Long lastMessageId;
}