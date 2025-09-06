package xyz.piod.keeper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRoomCreateRequest {
    @NotBlank(message = "Room name cannot be empty")
    @Size(min = 3, max = 50, message = "Room name must be between 3 and 50 characters")
    private String name;
}