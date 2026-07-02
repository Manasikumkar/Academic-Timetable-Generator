package com.itdept.timetable.service;

import com.itdept.timetable.model.*;
import com.itdept.timetable.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Seeds the database with the exact faculty, rooms and courses from your
 * IT Department Semester-II 2025-26 timetable on first startup.
 * Safe to run repeatedly - skips if data already exists.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final FacultyRepository  facultyRepo;
    private final RoomRepository     roomRepo;
    private final CourseRepository   courseRepo;
    private final ConstraintService  constraintService;

    @Override
    public void run(String... args) {
        if (facultyRepo.count() > 0) {
            log.info("Database already seeded, skipping.");
            return;
        }

        log.info("Seeding IT department data...");
        seedFaculty();
        seedRooms();
        seedCourses();
        constraintService.seedDefaults();
        log.info("Seeding complete.");
    }

    // ─── Faculty ─────────────────────────────────────────────────────────────
    private void seedFaculty() {
        List<Faculty> all = List.of(
            Faculty.builder().name("Dr. D. S. Hirolikar").shortCode("DSH")
                    .maxHoursPerDay(6).maxHoursPerWeek(24).unavailableSlots(List.of()).build(),
            Faculty.builder().name("Prof. A. B. Gadewar").shortCode("ABG")
                    .maxHoursPerDay(6).maxHoursPerWeek(24).unavailableSlots(List.of()).build(),
            Faculty.builder().name("Prof. Geetai Saindane").shortCode("GS")
                    .maxHoursPerDay(6).maxHoursPerWeek(24).unavailableSlots(List.of()).build(),
            Faculty.builder().name("Prof. Shikha Jain").shortCode("SJ")
                    .maxHoursPerDay(6).maxHoursPerWeek(24).unavailableSlots(List.of()).build(),
            Faculty.builder().name("Prof. S. Saraswat").shortCode("SS")
                    .maxHoursPerDay(6).maxHoursPerWeek(24).unavailableSlots(List.of()).build(),
            Faculty.builder().name("Prof. Nutan Phalakhe").shortCode("NNP")
                    .maxHoursPerDay(6).maxHoursPerWeek(24).unavailableSlots(List.of()).build(),
            Faculty.builder().name("Prof. Minal Gupta").shortCode("MG")
                    .maxHoursPerDay(6).maxHoursPerWeek(24).unavailableSlots(List.of()).build(),
            Faculty.builder().name("Prof. R. M. Kawale").shortCode("RMK")
                    .maxHoursPerDay(6).maxHoursPerWeek(24).unavailableSlots(List.of()).build(),
            Faculty.builder().name("Prof. Akanksha Ghodage").shortCode("AAG")
                    .maxHoursPerDay(6).maxHoursPerWeek(24).unavailableSlots(List.of()).build(),
            Faculty.builder().name("Prof. Jay Dudhal").shortCode("JD")
                    .maxHoursPerDay(6).maxHoursPerWeek(24).unavailableSlots(List.of()).build(),
            Faculty.builder().name("Prof. D. L. Mule").shortCode("DLM")
                    .maxHoursPerDay(4).maxHoursPerWeek(12).unavailableSlots(List.of()).build()
        );
        facultyRepo.saveAll(all);
        log.info("Saved {} faculty members", all.size());
    }

    // ─── Rooms ────────────────────────────────────────────────────────────────
    private void seedRooms() {
        List<Room> all = List.of(
            Room.builder().name("1333").type(Room.RoomType.CLASSROOM).capacity(60).build(),
            Room.builder().name("1332").type(Room.RoomType.CLASSROOM).capacity(60).build(),
            Room.builder().name("Software Lab").type(Room.RoomType.SOFTWARE_LAB).capacity(30).build(),
            Room.builder().name("Linux Lab").type(Room.RoomType.LINUX_LAB).capacity(30).build(),
            Room.builder().name("Project Lab").type(Room.RoomType.PROJECT_LAB).capacity(30).build()
        );
        roomRepo.saveAll(all);
        log.info("Saved {} rooms", all.size());
    }

    // ─── Courses ──────────────────────────────────────────────────────────────
    private void seedCourses() {
        // Fetch faculty by short code for easy assignment
        Map<String, Faculty> f = new java.util.HashMap<>();
        facultyRepo.findAll().forEach(fac -> f.put(fac.getShortCode(), fac));

        List<Course> all = new java.util.ArrayList<>();

        // ── SE Courses ───────────────────────────────────────────────────
        all.addAll(List.of(
            // Theory
            Course.builder().code("DBMS").fullName("Database Management System")
                    .type(Course.CourseType.THEORY).hoursPerWeek(4).credits(4)
                    .yearClass(Course.YearClass.SE).faculty(f.get("DSH")).build(),

            Course.builder().code("CG").fullName("Computer Graphics")
                    .type(Course.CourseType.THEORY).hoursPerWeek(4).credits(4)
                    .yearClass(Course.YearClass.SE).faculty(f.get("ABG")).build(),

            Course.builder().code("PS").fullName("Probability and Statistics")
                    .type(Course.CourseType.THEORY).hoursPerWeek(4).credits(4)
                    .yearClass(Course.YearClass.SE).faculty(f.get("GS")).build(),

            Course.builder().code("PA").fullName("Processor Architecture")
                    .type(Course.CourseType.THEORY).hoursPerWeek(4).credits(4)
                    .yearClass(Course.YearClass.SE).faculty(f.get("SJ")).build(),

            Course.builder().code("MIL").fullName("Modern Indian Language")
                    .type(Course.CourseType.THEORY).hoursPerWeek(2).credits(2)
                    .yearClass(Course.YearClass.SE).faculty(f.get("GS")).build(),

            Course.builder().code("OEII").fullName("Project Management")
                    .type(Course.CourseType.THEORY).hoursPerWeek(2).credits(2)
                    .yearClass(Course.YearClass.SE).faculty(f.get("SS")).build(),

            Course.builder().code("EVS").fullName("Environmental Studies")
                    .type(Course.CourseType.THEORY).hoursPerWeek(1).credits(0)
                    .yearClass(Course.YearClass.SE).faculty(f.get("AAG")).build(),

            Course.builder().code("ECOM").fullName("E-Commerce")
                    .type(Course.CourseType.THEORY).hoursPerWeek(2).credits(2)
                    .yearClass(Course.YearClass.SE).faculty(f.get("JD")).build(),

            // Labs
            Course.builder().code("DBMSL").fullName("Database Management System Lab")
                    .type(Course.CourseType.LAB).hoursPerWeek(2).credits(1)
                    .yearClass(Course.YearClass.SE).faculty(f.get("DSH")).build(),

            Course.builder().code("CGL").fullName("Computer Graphics Lab")
                    .type(Course.CourseType.LAB).hoursPerWeek(2).credits(1)
                    .yearClass(Course.YearClass.SE).faculty(f.get("ABG")).build(),

            Course.builder().code("DMSM").fullName("Digital Marketing and Social Media")
                    .type(Course.CourseType.LAB).hoursPerWeek(2).credits(1)
                    .yearClass(Course.YearClass.SE).faculty(f.get("SJ")).build(),

            Course.builder().code("MILL").fullName("Modern Indian Language Lab")
                    .type(Course.CourseType.LAB).hoursPerWeek(2).credits(1)
                    .yearClass(Course.YearClass.SE).faculty(f.get("GS")).build()
        ));

        // ── TE Courses ───────────────────────────────────────────────────
        all.addAll(List.of(
            Course.builder().code("WAD").fullName("Web Application Development")
                    .type(Course.CourseType.THEORY).hoursPerWeek(4).credits(4)
                    .yearClass(Course.YearClass.TE).faculty(f.get("SS")).build(),

            Course.builder().code("CNS").fullName("Computer Network and Security")
                    .type(Course.CourseType.THEORY).hoursPerWeek(4).credits(4)
                    .yearClass(Course.YearClass.TE).faculty(f.get("ABG")).build(),

            Course.builder().code("DSBDA").fullName("Data Science and Big Data Analytics")
                    .type(Course.CourseType.THEORY).hoursPerWeek(4).credits(4)
                    .yearClass(Course.YearClass.TE).faculty(f.get("SJ")).build(),

            Course.builder().code("SMDTE").fullName("Software Modelling and Design")
                    .type(Course.CourseType.THEORY).hoursPerWeek(4).credits(4)
                    .yearClass(Course.YearClass.TE).faculty(f.get("DSH")).build(),

            Course.builder().code("INTE").fullName("Internship")
                    .type(Course.CourseType.THEORY).hoursPerWeek(2).credits(2)
                    .yearClass(Course.YearClass.TE).faculty(f.get("NNP")).build(),

            Course.builder().code("AUDTE").fullName("Audit Course TE")
                    .type(Course.CourseType.THEORY).hoursPerWeek(1).credits(0)
                    .yearClass(Course.YearClass.TE).faculty(f.get("NNP")).build(),

            // Labs
            Course.builder().code("CNSL").fullName("Computer Networks and Security Lab")
                    .type(Course.CourseType.LAB).hoursPerWeek(2).credits(1)
                    .yearClass(Course.YearClass.TE).faculty(f.get("ABG")).build(),

            Course.builder().code("DSBDAL").fullName("Data Science and Big Data Analytics Lab")
                    .type(Course.CourseType.LAB).hoursPerWeek(2).credits(1)
                    .yearClass(Course.YearClass.TE).faculty(f.get("SJ")).build(),

            Course.builder().code("LPII").fullName("Laboratory Practice II")
                    .type(Course.CourseType.LAB).hoursPerWeek(2).credits(1)
                    .yearClass(Course.YearClass.TE).faculty(f.get("SS")).build()
        ));

        // ── BE Courses ───────────────────────────────────────────────────
        all.addAll(List.of(
            Course.builder().code("DS").fullName("Distributed Systems")
                    .type(Course.CourseType.THEORY).hoursPerWeek(4).credits(4)
                    .yearClass(Course.YearClass.BE).faculty(f.get("MG")).build(),

            Course.builder().code("ELECTV").fullName("Social Computing")
                    .type(Course.CourseType.THEORY).hoursPerWeek(4).credits(4)
                    .yearClass(Course.YearClass.BE).faculty(f.get("RMK")).build(),

            Course.builder().code("ELECTVI").fullName("Blockchain Technology")
                    .type(Course.CourseType.THEORY).hoursPerWeek(4).credits(4)
                    .yearClass(Course.YearClass.BE).faculty(f.get("NNP")).build(),

            Course.builder().code("SEP").fullName("Startup and Entrepreneurship")
                    .type(Course.CourseType.THEORY).hoursPerWeek(3).credits(3)
                    .yearClass(Course.YearClass.BE).faculty(f.get("DSH")).build(),

            Course.builder().code("AUDBE").fullName("Audit Course BE")
                    .type(Course.CourseType.THEORY).hoursPerWeek(1).credits(0)
                    .yearClass(Course.YearClass.BE).faculty(f.get("DLM")).build(),

            Course.builder().code("TPOBE").fullName("Training and Placement")
                    .type(Course.CourseType.THEORY).hoursPerWeek(1).credits(0)
                    .yearClass(Course.YearClass.BE).faculty(f.get("DLM")).build(),

            // Labs
            Course.builder().code("LPV").fullName("Laboratory Practice V")
                    .type(Course.CourseType.LAB).hoursPerWeek(2).credits(1)
                    .yearClass(Course.YearClass.BE).faculty(f.get("RMK")).build(),

            Course.builder().code("LPVI").fullName("Laboratory Practice VI")
                    .type(Course.CourseType.LAB).hoursPerWeek(2).credits(1)
                    .yearClass(Course.YearClass.BE).faculty(f.get("NNP")).build(),

            Course.builder().code("PROJECT").fullName("Project Work")
                    .type(Course.CourseType.LAB).hoursPerWeek(4).credits(6)
                    .yearClass(Course.YearClass.BE).faculty(f.get("DSH")).build()
        ));

        courseRepo.saveAll(all);
        log.info("Saved {} courses", all.size());
    }
}