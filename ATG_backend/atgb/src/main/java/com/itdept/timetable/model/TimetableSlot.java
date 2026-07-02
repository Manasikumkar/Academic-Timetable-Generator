package com.itdept.timetable.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "timetable_slots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long timetableVersionId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SchoolDay day;

    // For theory: slotNumber = slotStart = slotEnd (e.g. 1)
    // For lab:    slotStart = 5, slotEnd = 6 (2-hr block 14:00-16:00)
    @Column(nullable = false)
    private int slotNumber;   // primary slot (kept for backward compat)

    // columnDefinition avoids NOT NULL alter-table error on existing rows
    @Column(columnDefinition = "integer default 1")
    private int slotStart;    // start of block

    @Column(columnDefinition = "integer default 1")
    private int slotEnd;      // end of block (= slotStart for theory)

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Course.YearClass yearClass;

    // null = whole class; "I1" | "I2" | "I3" = lab batch
    private String division;

    private boolean labSession;

    // Semester: 1=Odd (I/III/V), 2=Even (II/IV/VI)
    @Column(columnDefinition = "integer default 2")
    private int semester;

    @Transient
    public String getStartTimeLabel() {
        return switch (slotStart) {
            case 1 -> "09:00"; case 2 -> "10:00"; case 3 -> "11:10";
            case 4 -> "12:10"; case 5 -> "14:00"; case 6 -> "15:00"; default -> "?";
        };
    }

    @Transient
    public String getEndTimeLabel() {
        return switch (slotEnd) {
            case 1 -> "10:00"; case 2 -> "11:00"; case 3 -> "12:10";
            case 4 -> "13:10"; case 5 -> "15:00"; case 6 -> "16:00"; default -> "?";
        };
    }

    @Transient
    public String getTimeLabel() {
        return getStartTimeLabel() + "–" + getEndTimeLabel();
    }

    public enum SchoolDay { MON, TUE, WED, THU, FRI, SAT }
}