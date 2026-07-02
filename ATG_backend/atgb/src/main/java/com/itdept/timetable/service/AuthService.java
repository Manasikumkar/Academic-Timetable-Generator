package com.itdept.timetable.service;

import com.itdept.timetable.dto.AuthRequest;
import com.itdept.timetable.dto.AuthResponse;
import com.itdept.timetable.model.User;
import com.itdept.timetable.repository.UserRepository;
import com.itdept.timetable.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired private UserRepository userRepo;
    @Autowired private JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthResponse register(String name, String email, String password, String role) {
        if (userRepo.findByEmail(email).isPresent()) {
            return new AuthResponse(false, "Email already exists", null, null, null);
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(encoder.encode(password));
        user.setRole(role.toUpperCase());
        userRepo.save(user);
        return new AuthResponse(true, "Registration successful", null, null, null);
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepo.findByEmail(request.getEmail()).orElse(null);
        if (user == null || !encoder.matches(request.getPassword(), user.getPassword())) {
            return new AuthResponse(false, "Invalid credentials", null, null, null);
        }
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        return new AuthResponse(true, "Login successful", token, user.getRole(), user.getName());
    }
}