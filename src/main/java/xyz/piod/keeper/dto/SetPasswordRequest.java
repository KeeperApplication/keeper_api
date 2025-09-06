package xyz.piod.keeper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetPasswordRequest(
        @NotBlank
        @Size(min = 6, message = "Password must be at least 6 characters long")
        String password,

        @NotBlank(message = "reCAPTCHA response is required")
        String recaptchaToken
) {
}