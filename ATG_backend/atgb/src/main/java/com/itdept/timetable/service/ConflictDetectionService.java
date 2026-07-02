package com.itdept.timetable.service;

import com.itdept.timetable.model.TimetableSlot;
import com.itdept.timetable.repository.TimetableSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ConflictDetectionService {

    private final TimetableSlotRepository slotRepository;

    public static class ConflictInfo {
        public String type;
        public String description;
        public TimetableSlot.SchoolDay day;
        public int slotNumber;
        public String timeLabel;

        public ConflictInfo(String type, String description,
                            TimetableSlot.SchoolDay day, int slot) {
            this.type = type;
            this.description = description;
            this.day = day;
            this.slotNumber = slot;
            this.timeLabel = timeLabel(slot);
        }

        private static String timeLabel(int s) {
            return switch (s) {
                case 1 -> "09:00-10:00";
                case 2 -> "10:00-11:00";
                case 3 -> "11:10-12:10";
                case 4 -> "12:10-13:10";
                case 5 -> "14:00-15:00";
                case 6 -> "15:00-16:00";
                default -> "?";
            };
        }
    }

    /**
     * Scans all saved slots for a version and returns a list of conflicts.
     */
    public List<ConflictInfo> detect(Long versionId) {
        List<TimetableSlot> slots = slotRepository.findByTimetableVersionId(versionId);
        List<ConflictInfo> conflicts = new ArrayList<>();

        // Build maps keyed by (day, slot, entityId)
        Map<String, List<TimetableSlot>> facultyMap = new HashMap<>();
        Map<String, List<TimetableSlot>> roomMap    = new HashMap<>();
        Map<String, List<TimetableSlot>> classMap   = new HashMap<>();

        for (TimetableSlot s : slots) {
            String fKey = s.getDay() + "-" + s.getSlotNumber() + "-F" + s.getFaculty().getId();
            String rKey = s.getDay() + "-" + s.getSlotNumber() + "-R" + s.getRoom().getId();
            String div  = s.getDivision() != null ? "-" + s.getDivision() : "";
            String cKey = s.getDay() + "-" + s.getSlotNumber() + "-" + s.getYearClass() + div;

            facultyMap.computeIfAbsent(fKey, k -> new ArrayList<>()).add(s);
            roomMap   .computeIfAbsent(rKey, k -> new ArrayList<>()).add(s);
            classMap  .computeIfAbsent(cKey, k -> new ArrayList<>()).add(s);
        }

        // Faculty clashes
        for (List<TimetableSlot> group : facultyMap.values()) {
            if (group.size() > 1) {
                TimetableSlot first = group.get(0);
                String names = group.stream()
                        .map(s -> s.getCourse().getCode())
                        .reduce((a, b) -> a + ", " + b).orElse("");
                conflicts.add(new ConflictInfo(
                        "FACULTY_CLASH",
                        "Faculty " + first.getFaculty().getShortCode() +
                        " assigned to multiple classes (" + names + ")",
                        first.getDay(), first.getSlotNumber()
                ));
            }
        }

        // Room clashes
        for (List<TimetableSlot> group : roomMap.values()) {
            if (group.size() > 1) {
                TimetableSlot first = group.get(0);
                String names = group.stream()
                        .map(s -> s.getCourse().getCode() + "(" + s.getYearClass() + ")")
                        .reduce((a, b) -> a + ", " + b).orElse("");
                conflicts.add(new ConflictInfo(
                        "ROOM_CLASH",
                        "Room " + first.getRoom().getName() +
                        " double-booked for (" + names + ")",
                        first.getDay(), first.getSlotNumber()
                ));
            }
        }

        // Class double-booking
        for (List<TimetableSlot> group : classMap.values()) {
            if (group.size() > 1) {
                TimetableSlot first = group.get(0);
                String names = group.stream()
                        .map(s -> s.getCourse().getCode())
                        .reduce((a, b) -> a + ", " + b).orElse("");
                conflicts.add(new ConflictInfo(
                        "CLASS_CLASH",
                        first.getYearClass() + " class scheduled for multiple subjects simultaneously (" + names + ")",
                        first.getDay(), first.getSlotNumber()
                ));
            }
        }

        return conflicts;
    }

    public int countHardConflicts(Long versionId) {
        return detect(versionId).size();
    }
}