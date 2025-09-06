package xyz.piod.keeper.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import xyz.piod.keeper.dto.AccountStatusResponse;
import xyz.piod.keeper.dto.AccountUpdateRequest;
import xyz.piod.keeper.dto.FcmTokenRequest;
import xyz.piod.keeper.dto.SetPasswordRequest;
import xyz.piod.keeper.dto.UserResponse;
import xyz.piod.keeper.entity.User;
import xyz.piod.keeper.mapper.UserMapper;
import xyz.piod.keeper.service.UserService;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;
    private final UserMapper userMapper;

    @PutMapping
    public ResponseEntity<UserResponse> updateAccount(
            @RequestBody @Valid AccountUpdateRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        User updatedUser = userService.updateUserAccount(principal.getUsername(), request);
        return ResponseEntity.ok(userMapper.toUserResponse(updatedUser));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails principal) {
        User user = userService.findUserByUsername(principal.getUsername());
        return ResponseEntity.ok(userMapper.toUserResponse(user));
    }

    @GetMapping("/status")
    public ResponseEntity<AccountStatusResponse> getAccountStatus(@AuthenticationPrincipal UserDetails principal) {
        User user = userService.findUserByUsername(principal.getUsername());
        boolean requiresSetup = user.getPassword() == null;
        return ResponseEntity.ok(new AccountStatusResponse(requiresSetup));
    }

    @PostMapping("/set-password")
    public ResponseEntity<Void> setPassword(@Valid @RequestBody SetPasswordRequest request, @AuthenticationPrincipal UserDetails principal) {
        userService.setPassword(principal.getUsername(), request.password(), request.recaptchaToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/fcm-token")
    public ResponseEntity<Void> saveFcmToken(@Valid @RequestBody FcmTokenRequest request, @AuthenticationPrincipal UserDetails principal) {
        userService.saveFcmToken(principal.getUsername(), request.getToken());
        return ResponseEntity.ok().build();
    }
}