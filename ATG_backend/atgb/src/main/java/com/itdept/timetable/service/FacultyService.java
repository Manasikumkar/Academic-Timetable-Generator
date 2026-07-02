package com.itdept.timetable.service;

import com.itdept.timetable.model.Faculty;
import com.itdept.timetable.repository.FacultyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FacultyService {

    private final FacultyRepository facultyRepository;

    public List<Faculty> getAll() {
        return facultyRepository.findAll();
    }

    public Faculty getById(Long id) {
        return facultyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found: " + id));
    }

    @Transactional
    public Faculty create(String name, String shortCode,
                          int maxHoursPerDay, int maxHoursPerWeek,
                          List<String> unavailableSlots) {

        Faculty faculty = Faculty.builder()
                .name(name)
                .shortCode(shortCode)
                .maxHoursPerDay(maxHoursPerDay > 0 ? maxHoursPerDay : 6)
                .maxHoursPerWeek(maxHoursPerWeek > 0 ? maxHoursPerWeek : 24)
                .unavailableSlots(unavailableSlots != null ? unavailableSlots : List.of())
                .build();

        return facultyRepository.save(faculty);
    }

    @Transactional
    public Faculty update(Long id, String name, String shortCode,
                          int maxHoursPerDay, int maxHoursPerWeek,
                          List<String> unavailableSlots) {

        Faculty faculty = getById(id);
        faculty.setName(name);
        faculty.setShortCode(shortCode);
        faculty.setMaxHoursPerDay(maxHoursPerDay > 0 ? maxHoursPerDay : 6);
        faculty.setMaxHoursPerWeek(maxHoursPerWeek > 0 ? maxHoursPerWeek : 24);
        faculty.setUnavailableSlots(unavailableSlots != null ? unavailableSlots : List.of());
        return facultyRepository.save(faculty);
    }

    @Transactional
    public void delete(Long id) {
        facultyRepository.deleteById(id);
    }
}