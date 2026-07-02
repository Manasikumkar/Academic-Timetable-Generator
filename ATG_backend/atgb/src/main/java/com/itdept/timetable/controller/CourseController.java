package com.itdept.timetable.controller;

import com.itdept.timetable.model.Course;
import com.itdept.timetable.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<List<Course>> getAll(
            @RequestParam(required = false) String yearClass) {
        if (yearClass != null) {
            return ResponseEntity.ok(
                    courseService.getByYear(Course.YearClass.valueOf(yearClass.toUpperCase())));
        }
        return ResponseEntity.ok(courseService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Course> getById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Course> create(@RequestBody Map<String, Object> body) {
        Course saved = courseService.create(
                (String) body.get("code"),
                (String) body.get("fullName"),
                Course.CourseType.valueOf((String) body.get("type")),
                (int) body.get("hoursPerWeek"),
                (int) body.get("credits"),
                Course.YearClass.valueOf((String) body.get("yearClass")),
                body.get("facultyId") != null
                        ? Long.valueOf(body.get("facultyId").toString()) : null
        );
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Course> update(@PathVariable Long id,
                                         @RequestBody Map<String, Object> body) {
        Course saved = courseService.update(
                id,
                (String) body.get("code"),
                (String) body.get("fullName"),
                Course.CourseType.valueOf((String) body.get("type")),
                (int) body.get("hoursPerWeek"),
                (int) body.get("credits"),
                Course.YearClass.valueOf((String) body.get("yearClass")),
                body.get("facultyId") != null
                        ? Long.valueOf(body.get("facultyId").toString()) : null
        );
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }
} 