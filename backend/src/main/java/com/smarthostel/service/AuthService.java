package com.smarthostel.service;

import com.smarthostel.dto.LoginRequest;
import com.smarthostel.dto.LoginResponse;
import com.smarthostel.dto.RegisterRequest;
import com.smarthostel.exception.BusinessRuleException;
import com.smarthostel.model.Student;
import com.smarthostel.repository.StudentRepository;
import com.smarthostel.security.AppUserDetails;
import com.smarthostel.security.JwtService;
import com.smarthostel.security.LoginRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginRateLimiter loginRateLimiter;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Login flow:
     * 1. Redis: is this IP blocked? (5 failures in 5 minutes -> HTTP 429)
     * 2. Spring Security checks the password against the BCrypt hash in Oracle
     * 3. wrong password -> count the failure in Redis -> HTTP 401
     * 4. correct password -> clear the counter and return a signed JWT
     */
    public LoginResponse login(LoginRequest request, String clientIp) {
        loginRateLimiter.checkAllowed(clientIp);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username().trim(), request.password()));
        } catch (BadCredentialsException e) {
            loginRateLimiter.recordFailure(clientIp);
            throw e;
        }
        loginRateLimiter.reset(clientIp);

        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(user);
        Instant expiresAt = jwtService.getExpiry(Instant.now());
        return new LoginResponse(token, expiresAt, user.getId(), user.getUsername(), user.getName(), user.getRole());
    }

    /** Student self-registration. The password is stored only as a BCrypt hash. */
    @Transactional
    public void registerStudent(RegisterRequest r) {
        if (studentRepository.existsByRegistrationNoIgnoreCase(r.registrationNo())) {
            throw new BusinessRuleException("Registration number " + r.registrationNo() + " is already registered");
        }
        if (studentRepository.existsByEmailIgnoreCase(r.email())) {
            throw new BusinessRuleException("E-mail " + r.email() + " is already registered");
        }
        Student s = new Student();
        s.setRegistrationNo(r.registrationNo().toUpperCase());
        s.setName(r.name().trim());
        s.setEmail(r.email().trim().toLowerCase());
        s.setPhone(r.phone());
        s.setGender(r.gender());
        s.setYear(r.year());
        s.setBranch(r.branch());
        s.setCgpa(r.cgpa());
        s.setPasswordHash(passwordEncoder.encode(r.password()));
        studentRepository.save(s);
    }
}
