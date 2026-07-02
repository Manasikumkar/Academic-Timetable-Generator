package com.itdept.timetable.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;       // e.g. "1333", "1332", "Software Lab", "Linux Lab", "Project Lab"

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RoomType type;     // CLASSROOM | SOFTWARE_LAB | LINUX_LAB | PROJECT_LAB

    private int capacity;

    public enum RoomType {
        CLASSROOM,
        SOFTWARE_LAB,
        LINUX_LAB,
        PROJECT_LAB
    }
}