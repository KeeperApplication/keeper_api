package xyz.piod.keeper.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.piod.keeper.dto.*;
import xyz.piod.keeper.entity.User;
import xyz.piod.keeper.service.AuthenticationService;
import xyz.piod.keeper.service.JwtService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;

    @PostMapping("/otp-login")
    public ResponseEntity<JwtResponse> processOtpLogin(@RequestBody OtpLoginRequest otpLoginRequest) {
        User user = authenticationService.loginOrRegisterLocalUser(otpLoginRequest.email());
        String jwtToken = jwtService.generateToken(user);
        return ResponseEntity.ok(new JwtResponse(jwtToken));
    }
}