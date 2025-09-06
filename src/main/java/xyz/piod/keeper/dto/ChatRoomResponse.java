package xyz.piod.keeper.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class ChatRoomResponse {
    private Long id;
    private String name;
    private String inviteCode;
    private UserResponse owner;
    private Set<UserResponse> participants;
    private LocalDateTime createdAt;
    private boolean isPrivate;
}