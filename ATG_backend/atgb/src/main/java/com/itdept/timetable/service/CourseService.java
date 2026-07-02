package com.itdept.timetable.service;

import com.itdept.timetable.model.Course;
import com.itdept.timetable.model.Faculty;
import com.itdept.timetable.repository.CourseRepository;
import com.itdept.timetable.repository.FacultyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final FacultyRepository facultyRepository;

    public List<Course> getAll() {
        return courseRepository.findAll();
    }

    public List<Course> getByYear(Course.YearClass yearClass) {
        return courseRepository.findByYearClass(yearClass);
    }

    public Course getById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found: " + id));
    }

    @Transactional
    public Course create(String code, String fullName, Course.CourseType type,
                         int hoursPerWeek, int credits, Course.YearClass yearClass,
                         Long facultyId) {

        Faculty faculty = facultyId != null
                ? facultyRepository.findById(facultyId)
                    .orElseThrow(() -> new RuntimeException("Faculty not found: " + facultyId))
                : null;

        Course course = Course.builder()
                .code(code)
                .fullName(fullName)
                .type(type)
                .hoursPerWeek(hoursPerWeek)
                .credits(credits)
                .yearClass(yearClass)
                .faculty(faculty)
                .build();

        return courseRepository.save(course);
    }

    @Transactional
    public Course update(Long id, String code, String fullName, Course.CourseType type,
                         int hoursPerWeek, int credits, Course.YearClass yearClass,
                         Long facultyId) {

        Course course = getById(id);
        course.setCode(code);
        course.setFullName(fullName);
        course.setType(type);
        course.setHoursPerWeek(hoursPerWeek);
        course.setCredits(credits);
        course.setYearClass(yearClass);

        if (facultyId != null) {
            Faculty faculty = facultyRepository.findById(facultyId)
                    .orElseThrow(() -> new RuntimeException("Faculty not found: " + facultyId));
            course.setFaculty(faculty);
        }

        return courseRepository.save(course);
    }

    @Transactional
    public void delete(Long id) {
        courseRepository.deleteById(id);
    }
}