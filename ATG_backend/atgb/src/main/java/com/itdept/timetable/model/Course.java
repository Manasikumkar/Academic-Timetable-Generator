package com.itdept.timetable.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;       // e.g. DBMS, CG, WAD

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CourseType type;   // THEORY | LAB

    @Column(nullable = false)
    private int hoursPerWeek;  // theory=4, lab=2 (means one 2-hr block per week)

    @Column(nullable = false)
    private int credits;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private YearClass yearClass; // SE | TE | BE

    // 0 = both semesters (Project, TPO)
    // 1 = Odd semester  (I / III / V)
    // 2 = Even semester (II / IV / VI)
    // columnDefinition avoids NOT NULL alter-table error on existing rows
    @Column(columnDefinition = "integer default 2")
    @Builder.Default
    private int semester = 2;

 @ManyToOne
 private Faculty faculty;

    public enum CourseType { THEORY, LAB }
    public enum YearClass  { SE, TE, BE }
}