package com.itdept.timetable.controller;

import com.itdept.timetable.model.Faculty;
import com.itdept.timetable.service.FacultyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/faculty")
@RequiredArgsConstructor
public class FacultyController {

    private final FacultyService facultyService;

    @GetMapping
    public ResponseEntity<List<Faculty>> getAll() {
        return ResponseEntity.ok(facultyService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Faculty> getById(@PathVariable Long id) {
        return ResponseEntity.ok(facultyService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Faculty> create(@RequestBody Map<String, Object> body) {
        Faculty saved = facultyService.create(
                (String) body.get("name"),
                (String) body.get("shortCode"),
                body.containsKey("maxHoursPerDay")  ? (int) body.get("maxHoursPerDay")  : 6,
                body.containsKey("maxHoursPerWeek") ? (int) body.get("maxHoursPerWeek") : 24,
                body.containsKey("unavailableSlots")
                        ? (List<String>) body.get("unavailableSlots") : List.of()
        );
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Faculty> update(@PathVariable Long id,
                                          @RequestBody Map<String, Object> body) {
        Faculty saved = facultyService.update(
                id,
                (String) body.get("name"),
                (String) body.get("shortCode"),
                body.containsKey("maxHoursPerDay")  ? (int) body.get("maxHoursPerDay")  : 6,
                body.containsKey("maxHoursPerWeek") ? (int) body.get("maxHoursPerWeek") : 24,
                body.containsKey("unavailableSlots")
                        ? (List<String>) body.get("unavailableSlots") : List.of()
        );
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        facultyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}