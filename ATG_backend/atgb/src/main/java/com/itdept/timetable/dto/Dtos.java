package com.itdept.timetable.dto;
import com.itdept.timetable.model.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

// ─── Course DTOs ─────────────────────────────────────────────────────────────

@Data
class CourseRequest {
    private String code;
    private String fullName;
    private Course.CourseType type;
    private int hoursPerWeek;
    private int credits;
    private Course.YearClass yearClass;
    private Long facultyId;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CourseResponse {
    private Long id;
    private String code;
    private String fullName;
    private Course.CourseType type;
    private int hoursPerWeek;
    private int credits;
    private Course.YearClass yearClass;
    private FacultyResponse faculty;

    public static CourseResponse from(Course c) {
        return CourseResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .fullName(c.getFullName())
                .type(c.getType())
                .hoursPerWeek(c.getHoursPerWeek())
                .credits(c.getCredits())
                .yearClass(c.getYearClass())
                .faculty(c.getFaculty() != null ? FacultyResponse.from(c.getFaculty()) : null)
                .build();
    }
}

// ─── Faculty DTOs ─────────────────────────────────────────────────────────────

@Data
class FacultyRequest {
    private String name;
    private String shortCode;
    private int maxHoursPerDay;
    private int maxHoursPerWeek;
    private List<String> unavailableSlots;  // ["MON-1", "SAT-5"]
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class FacultyResponse {
    private Long id;
    private String name;
    private String shortCode;
    private int maxHoursPerDay;
    private int maxHoursPerWeek;
    private List<String> unavailableSlots;

    public static FacultyResponse from(Faculty f) {
        return FacultyResponse.builder()
                .id(f.getId())
                .name(f.getName())
                .shortCode(f.getShortCode())
                .maxHoursPerDay(f.getMaxHoursPerDay())
                .maxHoursPerWeek(f.getMaxHoursPerWeek())
                .unavailableSlots(f.getUnavailableSlots())
                .build();
    }
}

// ─── Room DTOs ────────────────────────────────────────────────────────────────

@Data
class RoomRequest {
    private String name;
    private Room.RoomType type;
    private int capacity;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class RoomResponse {
    private Long id;
    private String name;
    private Room.RoomType type;
    private int capacity;

    public static RoomResponse from(Room r) {
        return RoomResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .type(r.getType())
                .capacity(r.getCapacity())
                .build();
    }
}

// ─── Timetable Slot DTO ───────────────────────────────────────────────────────

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class TimetableSlotResponse {
    private Long id;
    private TimetableSlot.SchoolDay day;
    private int slotNumber;
    private String timeLabel;
    private CourseResponse course;
    private FacultyResponse faculty;
    private RoomResponse room;
    private Course.YearClass yearClass;
    private String division;
    private boolean labSession;

    public static TimetableSlotResponse from(TimetableSlot s) {
        return TimetableSlotResponse.builder()
                .id(s.getId())
                .day(s.getDay())
                .slotNumber(s.getSlotNumber())
                .timeLabel(s.getTimeLabel())
                .course(CourseResponse.from(s.getCourse()))
                .faculty(FacultyResponse.from(s.getFaculty()))
                .room(RoomResponse.from(s.getRoom()))
                .yearClass(s.getYearClass())
                .division(s.getDivision())
                .labSession(s.isLabSession())
                .build();
    }
}

// ─── Timetable Version DTOs ───────────────────────────────────────────────────

@Data
class GenerateRequest {
    private String name;
    private String academicYear;
    private int semester;
    // Optional GA overrides
    private Integer populationSize;
    private Integer maxGenerations;
    private Double mutationRate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class TimetableVersionResponse {
    private Long id;
    private String name;
    private String academicYear;
    private int semester;
    private TimetableVersion.Status status;
    private double fitnessScore;
    private int hardConflicts;
    private int generations;
    private LocalDateTime createdAt;
    private LocalDateTime deployedAt;

    public static TimetableVersionResponse from(TimetableVersion v) {
        return TimetableVersionResponse.builder()
                .id(v.getId())
                .name(v.getName())
                .academicYear(v.getAcademicYear())
                .semester(v.getSemester())
                .status(v.getStatus())
                .fitnessScore(v.getFitnessScore())
                .hardConflicts(v.getHardConflicts())
                .generations(v.getGenerations())
                .createdAt(v.getCreatedAt())
                .deployedAt(v.getDeployedAt())
                .build();
    }
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class FullTimetableResponse {
    private TimetableVersionResponse version;
    private List<TimetableSlotResponse> slots;
    private List<ConflictInfo> conflicts;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ConflictInfo {
    private String type;        // FACULTY_CLASH | ROOM_CLASH | CLASS_CLASH
    private String description;
    private TimetableSlot.SchoolDay day;
    private int slotNumber;
    private String timeLabel;
}

// ─── Constraint DTO ───────────────────────────────────────────────────────────

@Data
class ConstraintRequest {
    private Constraint.ConstraintType type;
    private String name;
    private String description;
    private int penalty;
    private boolean active;
}

// ─── GA Progress ─────────────────────────────────────────────────────────────

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class GaProgressResponse {
    private int currentGeneration;
    private int maxGenerations;
    private double bestFitness;
    private int hardConflicts;
    private boolean complete;
    private List<Double> fitnessHistory;
}           