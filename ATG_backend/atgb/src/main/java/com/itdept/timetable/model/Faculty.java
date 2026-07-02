package com.itdept.timetable.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "faculty")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Faculty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;        // e.g. Dr. D. S. Hirolikar

    @Column(nullable = false, unique = true)
    private String shortCode;   // e.g. DSH, ABG, GS, SJ, SS, NNP, MG, RMK, AAG, JD

    @Builder.Default
    private int maxHoursPerDay = 6;

    @Builder.Default
    private int maxHoursPerWeek = 24;

    // Specific day+slot strings where faculty is NOT available
    // Format: "MON-1", "TUE-3" etc.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "faculty_unavailable_slots",
                     joinColumns = @JoinColumn(name = "faculty_id"))
    @Column(name = "slot_key")
    @Builder.Default
    private List<String> unavailableSlots = new ArrayList<>();
}