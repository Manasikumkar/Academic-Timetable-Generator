package com.itdept.timetable.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "constraints")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Constraint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ConstraintType type;   // HARD | SOFT

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    // Weight used in fitness function (HARD = always applied, SOFT uses this weight)
    @Builder.Default
    private int penalty = 10;

    @Builder.Default
    private boolean active = true;

    public enum ConstraintType { HARD, SOFT }
}