package com.itdept.timetable.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "timetable_versions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;         // e.g. "Sem-II 2025-26"
    private String academicYear; // e.g. "2025-26"

    // 1 = Odd semester (I/III/V), 2 = Even semester (II/IV/VI)
    private int semester;

    // SE=2, TE=3, BE=4 — which years are included
    // Stored as comma-separated: "SE,TE,BE"
    @Column(columnDefinition = "varchar(20) default 'SE,TE,BE'")
    @Builder.Default
    private String includedYears = "SE,TE,BE";

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.DRAFT;
    @ElementCollection
    private List<String> conflicts;

    private double fitnessScore;
    private int hardConflicts;
    private int generations;

    private LocalDateTime createdAt;
    private LocalDateTime deployedAt;

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); }

    public enum Status { DRAFT, FINAL, DEPLOYED }
}