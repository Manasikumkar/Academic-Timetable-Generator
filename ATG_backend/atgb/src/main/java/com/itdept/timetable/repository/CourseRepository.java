package com.itdept.timetable.repository;

import com.itdept.timetable.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByYearClass(Course.YearClass yearClass);
    List<Course> findByYearClassAndType(Course.YearClass yearClass, Course.CourseType type);
    Optional<Course> findByCode(String code);
}