package xyz.piod.keeper.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.piod.keeper.dto.*;
import xyz.piod.keeper.entity.User;
import xyz.piod.keeper.service.AuthenticationService;
import xyz.piod.keeper.service.JwtService;
import xyz.piod.keeper.service.UserService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    @PostMapping("/check-status")
    public ResponseEntity<UserStatusResponse> checkUserStatus(@Valid @RequestBody EmailStatusRequest request) {
        UserLoginStatus status = authenticationService.checkUserStatus(request.email());
        return ResponseEntity.ok(new UserStatusResponse(status));
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> authenticateUser(@RequestBody LoginRequest loginRequest) {
        User user = userService.findUserByEmail(loginRequest.email());

        if (user.getPassword() == null) {
            throw new BadCredentialsException("Account not fully set up. Please use the initial login method.");
        }

        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
        );

        String jwtToken = jwtService.generateToken(user);
        return ResponseEntity.ok(new JwtResponse(jwtToken));
    }

    @PostMapping("/otp-login")
    public ResponseEntity<JwtResponse> processOtpLogin(@RequestBody OtpLoginRequest otpLoginRequest) {
        User user = authenticationService.loginOrRegisterLocalUser(otpLoginRequest.email());
        String jwtToken = jwtService.generateToken(user);
        return ResponseEntity.ok(new JwtResponse(jwtToken));
    }
}