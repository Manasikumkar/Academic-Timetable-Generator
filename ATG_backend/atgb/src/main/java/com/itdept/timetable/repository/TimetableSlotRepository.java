package com.itdept.timetable.repository;

import com.itdept.timetable.model.Course;
import com.itdept.timetable.model.TimetableSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface TimetableSlotRepository extends JpaRepository<TimetableSlot, Long> {

    List<TimetableSlot> findByTimetableVersionId(Long versionId);

    List<TimetableSlot> findByTimetableVersionIdAndYearClass(
            Long versionId, Course.YearClass yearClass);

    // Detect faculty clash: same faculty, same day, same slot, same version
    @Query("SELECT s FROM TimetableSlot s " +
           "WHERE s.timetableVersionId = :vid " +
           "AND s.faculty.id = :fid " +
           "AND s.day = :day " +
           "AND s.slotNumber = :slot")
    List<TimetableSlot> findFacultyClashes(
            @Param("vid")  Long versionId,
            @Param("fid")  Long facultyId,
            @Param("day")  TimetableSlot.SchoolDay day,
            @Param("slot") int slotNumber);

    // Detect room clash: same room, same day, same slot
    @Query("SELECT s FROM TimetableSlot s " +
           "WHERE s.timetableVersionId = :vid " +
           "AND s.room.id = :rid " +
           "AND s.day = :day " +
           "AND s.slotNumber = :slot")
    List<TimetableSlot> findRoomClashes(
            @Param("vid")  Long versionId,
            @Param("rid")  Long roomId,
            @Param("day")  TimetableSlot.SchoolDay day,
            @Param("slot") int slotNumber);

    // Count how many times a faculty is scheduled per day in a version
    @Query("SELECT COUNT(s) FROM TimetableSlot s " +
           "WHERE s.timetableVersionId = :vid " +
           "AND s.faculty.id = :fid " +
           "AND s.day = :day")
    long countFacultySessionsOnDay(
            @Param("vid") Long versionId,
            @Param("fid") Long facultyId,
            @Param("day") TimetableSlot.SchoolDay day);

    @Modifying
    @Transactional
    @Query("DELETE FROM TimetableSlot s WHERE s.timetableVersionId = :vid")
    void deleteByTimetableVersionId(@Param("vid") Long versionId);
}