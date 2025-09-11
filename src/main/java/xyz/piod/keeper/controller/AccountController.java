package xyz.piod.keeper.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import xyz.piod.keeper.dto.*;
import xyz.piod.keeper.entity.User;
import xyz.piod.keeper.mapper.UserMapper;
import xyz.piod.keeper.service.AuthenticationService;
import xyz.piod.keeper.service.UserService;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final UserMapper userMapper;

    @PutMapping
    public ResponseEntity<UserResponse> updateAccount(
            @RequestBody @Valid AccountUpdateRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        User updatedUser = authenticationService.updateUserAccount(principal.getUsername(), request);
        return ResponseEntity.ok(userMapper.toUserResponse(updatedUser));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails principal) {
        User user = userService.findUserByUsername(principal.getUsername());
        return ResponseEntity.ok(userMapper.toUserResponse(user));
    }

    @PostMapping("/fcm-token")
    public ResponseEntity<Void> registerFcmToken(@RequestBody @Valid FcmTokenRequest request,
                                                 @AuthenticationPrincipal UserDetails principal) {
        userService.updateFcmToken(principal.getUsername(), request.getToken());
        return ResponseEntity.ok().build();
    }
}