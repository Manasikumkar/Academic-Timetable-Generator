package com.itdept.timetable.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itdept.timetable.model.TimetableSlot;
import com.itdept.timetable.repository.CourseRepository;
import com.itdept.timetable.repository.FacultyRepository;
import com.itdept.timetable.repository.RoomRepository;
import com.itdept.timetable.repository.TimetableSlotRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/timetable")
@RequiredArgsConstructor
public class TimetableSlotController {
	 private final TimetableSlotRepository slotRepository;
	    private final CourseRepository courseRepository;
	    private final FacultyRepository facultyRepository;
	    private final RoomRepository roomRepository;
	    
	    @PutMapping("/slot/{id}")
	    public TimetableSlot updateSlot(@PathVariable Long id,
	                                   @RequestBody Map<String, Long> payload) {

	        TimetableSlot slot = slotRepository.findById(id)
	                .orElseThrow(() -> new RuntimeException("Slot not found"));

	        // 🔹 get new values
	        Long courseId = payload.get("courseId");
	        Long facultyId = payload.get("facultyId");
	        Long roomId = payload.get("roomId");

	        // 🔹 fetch entities
	        if (courseId != null) {
	            slot.setCourse(courseRepository.findById(courseId).orElse(null));
	        }

	        if (facultyId != null) {
	            slot.setFaculty(facultyRepository.findById(facultyId).orElse(null));
	        }

	        if (roomId != null) {
	            slot.setRoom(roomRepository.findById(roomId).orElse(null));
	        }

	        return slotRepository.save(slot);
	    }
	    @PostMapping("/slot")
	    public TimetableSlot createSlot(@RequestBody Map<String, Object> body) {

	        TimetableSlot slot = new TimetableSlot();

	        // 🔹 basic
	        slot.setDay(TimetableSlot.SchoolDay.valueOf((String) body.get("day")));
	        slot.setSlotNumber(((Number) body.get("slotNumber")).intValue());
	        slot.setYearClass(
	            com.itdept.timetable.model.Course.YearClass.valueOf((String) body.get("yearClass"))
	        );

	        // 🔹 relations
	        Long courseId = ((Number) body.get("courseId")).longValue();
	        Long facultyId = ((Number) body.get("facultyId")).longValue();
	        Long roomId = ((Number) body.get("roomId")).longValue();

	        slot.setCourse(courseRepository.findById(courseId).orElse(null));
	        slot.setFaculty(facultyRepository.findById(facultyId).orElse(null));
	        slot.setRoom(roomRepository.findById(roomId).orElse(null));

	        slot.setLabSession(false);

	        // 🔥🔥 MOST IMPORTANT FIX
	        Long versionId = ((Number) body.get("timetableVersionId")).longValue();
	        slot.setTimetableVersionId(versionId);

	        return slotRepository.save(slot);
	    }
}
