package com.itdept.timetable.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;   // store encoded password
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String role;   // "ADMIN", "TEACHER", "STUDENT"
}