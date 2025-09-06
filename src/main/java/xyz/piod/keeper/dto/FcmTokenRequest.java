package xyz.piod.keeper.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class FcmTokenRequest {
    @NotBlank
    private String token;
}