package xyz.piod.keeper.dto;

import lombok.Data;

@Data
public class RepliedMessageInfo {
    private Long id;
    private String senderUsername;
    private String content;
}