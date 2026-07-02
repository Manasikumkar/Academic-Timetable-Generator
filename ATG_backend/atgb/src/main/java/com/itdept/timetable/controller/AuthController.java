package com.itdept.timetable.controller;

import com.itdept.timetable.dto.AuthRequest;
import com.itdept.timetable.dto.AuthResponse;
import com.itdept.timetable.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {
    @Autowired private AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest req) {
        return authService.login(req);
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestParam String name,
                                 @RequestParam String email,
                                 @RequestParam String password,
                                 @RequestParam String role) {
        return authService.register(name, email, password, role);
    }
}