package com.smarthostel.controller;

import com.smarthostel.dto.*;
import com.smarthostel.security.AppUserDetails;
import com.smarthostel.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Public. Rate-limited per IP address (see LoginRateLimiter). */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request, http.getRemoteAddr());
    }

    /** Public. Student self-registration. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
        authService.registerStudent(request);
        return new MessageResponse("Registration successful. You can now log in with your registration number.");
    }

    /** Who am I? (reads the user from the JWT) */
    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal AppUserDetails user) {
        return new CurrentUserResponse(user.getId(), user.getUsername(), user.getName(), user.getRole());
    }
}
