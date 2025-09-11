package xyz.piod.keeper.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AccountUpdateRequest {
    @Size(max = 15, message = "Username must not exceed 15 characters")
    private String username;
    private String profilePicture;
}