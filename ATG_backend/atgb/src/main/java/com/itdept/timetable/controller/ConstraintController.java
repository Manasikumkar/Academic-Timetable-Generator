package com.itdept.timetable.controller;

import com.itdept.timetable.model.Constraint;
import com.itdept.timetable.service.ConstraintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/constraints")
@RequiredArgsConstructor
public class ConstraintController {

    private final ConstraintService constraintService;

    @GetMapping
    public ResponseEntity<List<Constraint>> getAll() {
        return ResponseEntity.ok(constraintService.getAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Constraint>> getActive() {
        return ResponseEntity.ok(constraintService.getActive());
    }

    @PostMapping
    public ResponseEntity<Constraint> create(@RequestBody Map<String, Object> body) {
        Constraint saved = constraintService.create(
                Constraint.ConstraintType.valueOf((String) body.get("type")),
                (String) body.get("name"),
                (String) body.get("description"),
                body.containsKey("penalty") ? (int) body.get("penalty") : 10,
                body.containsKey("active") ? (boolean) body.get("active") : true
        );
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Constraint> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(constraintService.toggleActive(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        constraintService.delete(id);
        return ResponseEntity.noContent().build();
    }
}