package com.example.zadanie.service;

import com.example.zadanie.api.dto.LoginRequest;
import com.example.zadanie.api.dto.LoginResponse;
import com.example.zadanie.entity.User;
import com.example.zadanie.exception.AuthenticationException;
import com.example.zadanie.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Invalid email or password");
        }

        // Generate JWT token
        String token = jwtService.generateToken(user.getEmail(), user.getId());

        // Return response with token
        return new LoginResponse(token, user.getEmail(), user.getId());
    }
}
