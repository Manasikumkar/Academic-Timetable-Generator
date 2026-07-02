package com.itdept.timetable.service;

import com.itdept.timetable.algorithm.GeneticAlgorithm;
import com.itdept.timetable.algorithm.GeneticAlgorithm.*;
import com.itdept.timetable.config.GaProperties;
import com.itdept.timetable.model.*;
import com.itdept.timetable.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimetableGenerationService {

    private final GeneticAlgorithm         geneticAlgorithm;
    private final GaProperties             gaProperties;
    private final CourseRepository         courseRepository;
    private final RoomRepository           roomRepository;
    private final TimetableVersionRepository versionRepository;
    private final TimetableSlotRepository  slotRepository;
    private final ConflictDetectionService conflictService;
    

    /**
     * Generate a timetable for a specific semester.
     *
     * @param semester   1=Odd (I/III/V), 2=Even (II/IV/VI)
     * @param years      comma-separated years to include e.g. "SE,TE,BE"
     */
    @Transactional
    public TimetableVersion generate(String name, String academicYear,
                                     int semester, String years,
                                     Integer popOverride, Integer genOverride, Double mutOverride) {

        log.info("Generating timetable: {} sem={} years={}", name, semester, years);

        // Load courses filtered by semester
        List<Course> courses = loadCoursesForSemester(semester, years);
        List<Room>   rooms   = roomRepository.findAll();

        if (courses.isEmpty()) throw new RuntimeException(
            "No courses found for Semester " + semester + ". Please add courses for this semester.");
        if (rooms.isEmpty()) throw new RuntimeException("No rooms configured.");

        log.info("Loaded {} courses for semester {}", courses.size(), semester);

        // Build GA config
        GaConfig cfg = new GaConfig();
        cfg.populationSize = popOverride != null ? popOverride : gaProperties.getPopulationSize();
        cfg.maxGenerations = genOverride != null ? genOverride : gaProperties.getMaxGenerations();
        cfg.mutationRate   = mutOverride != null ? mutOverride : gaProperties.getMutationRate();
        cfg.crossoverRate  = gaProperties.getCrossoverRate();
        cfg.eliteCount     = gaProperties.getEliteCount();
        cfg.tournamentSize = gaProperties.getTournamentSize();

        // Run GA
        GaResult result = geneticAlgorithm.run(cfg, courses, rooms);
       
        repairBackToBack(result.best);
       
        
        List<String> conflicts = geneticAlgorithm.detectConflicts(result.best);
        
        

        System.out.println("CONFLICTS SIZE: " + conflicts.size());
        if (result.best == null) throw new RuntimeException("GA failed to produce a solution.");

//        int hardConflicts = geneticAlgorithm.countHard(result.best);
        int hardConflicts = conflicts.size();

        // Save version
        TimetableVersion version = TimetableVersion.builder()
            .name(name)
            .academicYear(academicYear)
            .semester(semester)
            .includedYears(years)
            .status(TimetableVersion.Status.DRAFT)
            .fitnessScore(result.best.fitness)
            .hardConflicts(hardConflicts)
            .generations(result.generationsRun)
            .conflicts(conflicts)   // 🔥 ADD
            .build();
        version = versionRepository.save(version);

        // Save slots — lab genes produce 2 TimetableSlot rows (one per slot in block)
        List<TimetableSlot> slots = new ArrayList<>();
        final Long vId = version.getId();

        for (Gene gene : result.best.genes) {
            if (gene.faculty == null || gene.room == null || gene.course == null) continue;

            if (gene.labSession) {
                // Create TWO slots for the 2-hr lab block
                for (int s = gene.slotStart; s <= gene.slotEnd; s++) {
                    slots.add(TimetableSlot.builder()
                        .timetableVersionId(vId)
                        .day(gene.day)
                        .slotNumber(s)
                        .slotStart(gene.slotStart)
                        .slotEnd(gene.slotEnd)
                        .course(gene.course)
                        .faculty(gene.faculty)
                        .room(gene.room)
                        .yearClass(gene.yearClass)
                        .division(gene.division)
                        .labSession(true)
                        .semester(semester)
                        .build());
                }
            } else {
                slots.add(TimetableSlot.builder()
                    .timetableVersionId(vId)
                    .day(gene.day)
                    .slotNumber(gene.slotStart)
                    .slotStart(gene.slotStart)
                    .slotEnd(gene.slotStart)
                    .course(gene.course)
                    .faculty(gene.faculty)
                    .room(gene.room)
                    .yearClass(gene.yearClass)
                    .division(null)
                    .labSession(false)
                    .semester(semester)
                    .build());
            }
        }

        slotRepository.saveAll(slots);
        log.info("Saved {} slots for version {} (hardConflicts={})", slots.size(), vId, hardConflicts);
        return version;
    }

    /**
     * Load courses for the given semester.
     * Semester 1 = Odd (courses tagged semester=1)
     * Semester 2 = Even (courses tagged semester=2)
     * If courses have no semester tag (legacy), load all.
     */
    private List<Course> loadCoursesForSemester(int semester, String years) {
        List<String> yearList = List.of(years.split(","));
        List<Course> all = courseRepository.findAll();

        return all.stream()
            .filter(c -> {
                // Year filter
                if (!yearList.contains(c.getYearClass().name())) return false;
                // Semester filter: if course has semester set, match it; else include all
                if (c.getSemester() == 0) return true;  // untagged → include always
                return c.getSemester() == semester;
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
   

    public List<TimetableSlot> getSlots(Long versionId, Course.YearClass yearClass) {
        if (yearClass != null)
            return slotRepository.findByTimetableVersionIdAndYearClass(versionId, yearClass);
        return slotRepository.findByTimetableVersionId(versionId);
    }

    public List<TimetableVersion> getAllVersions() {
        return versionRepository.findAllByOrderByCreatedAtDesc();
    }

    public TimetableVersion getVersion(Long id) {
        return versionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Version not found: " + id));
    }

    @Transactional
    public TimetableVersion updateStatus(Long versionId, TimetableVersion.Status status) {
        TimetableVersion v = getVersion(versionId);
        v.setStatus(status);
        if (status == TimetableVersion.Status.DEPLOYED) v.setDeployedAt(LocalDateTime.now());
        return versionRepository.save(v);
    }

    @Transactional
    public void deleteVersion(Long versionId) {
        slotRepository.deleteByTimetableVersionId(versionId);
        versionRepository.deleteById(versionId);
    }
    private void repairBackToBack(Chromosome chrom) {

        // index for quick checks
        Map<String, Map<String, Map<Integer, Gene>>> grid = new HashMap<>();

        for (Gene g : chrom.genes) {
            if (g.labSession) continue;

            String cls = g.yearClass + (g.division != null ? g.division : "");
            grid.putIfAbsent(cls, new HashMap<>());

            Map<String, Map<Integer, Gene>> dayMap = grid.get(cls);
            dayMap.putIfAbsent(g.day.name(), new HashMap<>());

            dayMap.get(g.day.name()).put(g.slotStart, g);
        }

        for (String cls : grid.keySet()) {
            for (String day : grid.get(cls).keySet()) {

                Map<Integer, Gene> slots = grid.get(cls).get(day);

                for (int s = 1; s <= 5; s++) {

                    Gene g1 = slots.get(s);
                    Gene g2 = slots.get(s + 1);

                    if (g1 == null || g2 == null) continue;

                    if (!g1.course.getCode().equals(g2.course.getCode())) continue;

                    // 🔥 try to move g2 to another safe place
                    for (Gene candidate : chrom.genes) {

                        if (candidate.labSession) continue;

                        // different day preferred
                        if (candidate.day == g2.day) continue;

                        // avoid same subject
                        if (candidate.course.getCode().equals(g2.course.getCode())) continue;

                        // 🔒 HARD CHECKS
                        if (willClash(chrom, candidate, g2.day, g2.slotStart)) continue;
                        if (willClash(chrom, g2, candidate.day, candidate.slotStart)) continue;

                        // ✅ SAFE SWAP
                        TimetableSlot.SchoolDay tempDay = candidate.day;
                        int tempSlot = candidate.slotStart;

                        candidate.day = g2.day;
                        candidate.slotStart = g2.slotStart;
                        candidate.slotEnd = g2.slotEnd;

                        g2.day = tempDay;
                        g2.slotStart = tempSlot;
                        g2.slotEnd = tempSlot;

                        break;
                    }
                }
            }
        }
        
    }
    private boolean willClash(Chromosome chrom, Gene g, 
            TimetableSlot.SchoolDay newDay, int newSlot) {

        for (Gene other : chrom.genes) {

            if (other == g) continue;

            if (other.slotStart == newSlot || other.slotEnd == newSlot)  {

                // faculty clash
                if (other.faculty.getId().equals(g.faculty.getId())) return true;

                // room clash
                if (other.room.getId().equals(g.room.getId())) return true;

                // class clash
                if (other.yearClass == g.yearClass &&
                    Objects.equals(other.division, g.division)) return true;
                
             // same subject
                if (other.course.getCode().equals(g.course.getCode())) return true;
            }
        }

        return false;
    }
    
    
}