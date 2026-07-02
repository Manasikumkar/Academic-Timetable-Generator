package com.itdept.timetable.repository;

import com.itdept.timetable.model.TimetableVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimetableVersionRepository extends JpaRepository<TimetableVersion, Long> {

    List<TimetableVersion> findByStatusOrderByCreatedAtDesc(TimetableVersion.Status status);

    Optional<TimetableVersion> findFirstByStatusOrderByCreatedAtDesc(TimetableVersion.Status status);

    List<TimetableVersion> findAllByOrderByCreatedAtDesc();
}