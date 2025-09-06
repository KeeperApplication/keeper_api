package xyz.piod.keeper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PendingRequestResponse {
    private UserResponse user;
    private RequestDirection direction;

    public enum RequestDirection {
        INCOMING,
        OUTGOING
    }
}