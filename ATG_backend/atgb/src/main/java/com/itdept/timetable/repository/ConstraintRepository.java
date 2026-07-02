package com.itdept.timetable.repository;

import com.itdept.timetable.model.Constraint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConstraintRepository extends JpaRepository<Constraint, Long> {
    List<Constraint> findByActiveTrue();
    List<Constraint> findByTypeAndActiveTrue(Constraint.ConstraintType type);
}