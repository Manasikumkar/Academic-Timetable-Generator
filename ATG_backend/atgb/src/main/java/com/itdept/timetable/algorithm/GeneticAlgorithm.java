package com.itdept.timetable.algorithm;

import com.itdept.timetable.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GeneticAlgorithm {
	
	private List<Course> allCourses; 

    public static final TimetableSlot.SchoolDay[] DAYS = TimetableSlot.SchoolDay.values();
    public static final int[][] LAB_PAIRS = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
    public static final int[] THEORY_SLOTS = { 1, 2, 3, 4, 5, 6 };

    private static final Set<String> BLOCKED = Set.of();
    private static final int SAT_MAX = 4;   // Saturday only slots 1-4
    private static final Random RNG = new Random();

    // ── Entry point ────────────────────────────────────────────────────────
    public GaResult run(GaConfig cfg, List<Course> courses, List<Room> rooms) {
    	this.allCourses = courses;
        List<Room> classrooms = byType(rooms, Room.RoomType.CLASSROOM);
        List<Room> swLabs = byType(rooms, Room.RoomType.SOFTWARE_LAB);
        List<Room> linLabs = byType(rooms, Room.RoomType.LINUX_LAB);
        List<Room> projLabs = byType(rooms, Room.RoomType.PROJECT_LAB);

        log.info("GA v6 | courses={} pop={} gen={}", courses.size(), cfg.populationSize, cfg.maxGenerations);

        List<Chromosome> pop = new ArrayList<>();
        for (int i = 0; i < cfg.populationSize; i++)
            pop.add(buildChromosome(courses, classrooms, swLabs, linLabs, projLabs));

        Chromosome best = null;
        List<Double> history = new ArrayList<>();

        for (int gen = 0; gen < cfg.maxGenerations; gen++) {
            for (Chromosome c : pop)
                if (c.fitness <= 0)
                    c.fitness = evaluate(c);
            pop.sort((a, b) -> Double.compare(b.fitness, a.fitness));
            Chromosome gb = pop.get(0);
            if (best == null || gb.fitness > best.fitness)
                best = gb.deepCopy();
            history.add(best.fitness);

            int hard = countHard(best);
            if (gen % 50 == 0)
                log.info("Gen {} | fit={:.0f} | hard={}", gen, best.fitness, hard);
            if (hard == 0) {
                List<String> conflicts = detectConflicts(best);
                log.info("TOTAL CONFLICTS: {}", conflicts.size());
                return new GaResult(best, gen + 1, history);
            }

            List<Chromosome> next = new ArrayList<>();
            for (int e = 0; e < cfg.eliteCount && e < pop.size(); e++)
                next.add(pop.get(e).deepCopy());
            while (next.size() < cfg.populationSize) {
                Chromosome p1 = ts(pop, cfg.tournamentSize);
                Chromosome child = RNG.nextDouble() < cfg.crossoverRate ? crossover(p1, ts(pop, cfg.tournamentSize))
                        : p1.deepCopy();
                if (RNG.nextDouble() < cfg.mutationRate)
                    mutate(child);
                repair(child, classrooms, swLabs, linLabs, projLabs);
                child.fitness = 0;
                next.add(child);
            }
            pop = next;
        }
        log.info("GA done | fit={:.0f} hard={}", best != null ? best.fitness : 0, countHard(best));
        return new GaResult(best, cfg.maxGenerations, history);
    }

    // ── BuildChromosome: LAB logic unchanged (5 blocks), THEORY with retry ──
    private Chromosome buildChromosome(List<Course> courses, List<Room> classrooms, List<Room> sw, List<Room> lin,
            List<Room> proj) {

        Chromosome chrom = new Chromosome();
        Set<String> classUsed = new HashSet<>();
        Set<String> facultyUsed = new HashSet<>();
        Set<String> roomUsed = new HashSet<>();
        Map<String, Integer> dayLabCount = new HashMap<>();

        // === LAB PLACEMENT (exactly as original, tries 5 blocks per year) ===
        List<Course> labCourses = courses.stream().filter(c -> c.getType() == Course.CourseType.LAB)
                .collect(Collectors.toList());
        Map<Course.YearClass, List<Course>> labGroups = labCourses.stream()
                .collect(Collectors.groupingBy(Course::getYearClass));

        for (Course.YearClass year : labGroups.keySet()) {
            List<Course> labs = labGroups.get(year);
            if (labs.size() < 3)
                continue;
            int attempts = 0, placed = 0;
            while (placed < 5 && attempts < 25) {
                boolean success = placeLabBlockGroup(chrom, labs, year, sw, lin, proj,
                        classUsed, facultyUsed, roomUsed, dayLabCount);
                if (success)
                    placed++;
                attempts++;
            }
        }

        // === THEORY PLACEMENT (sorted by hours descending) ===
        List<Course> theoryCourses = courses.stream()
                .filter(c -> c.getType() != Course.CourseType.LAB && c.getFaculty() != null)
                // CHANGED: use correctHours() for sort so high-priority subjects are placed first
                .sorted((a, b) -> Integer.compare(correctHours(b), correctHours(a)))
                .collect(Collectors.toList());

        // First pass: use the two-pass placeTheory (spread across days)
        for (Course course : theoryCourses) {
            placeTheory(chrom, course, classrooms, classUsed, facultyUsed, roomUsed);
        }

        // === RETRY PASS for subjects that still need more hours ===
        // Count placed hours
        Map<String, Integer> placedHours = new HashMap<>();
        for (Gene g : chrom.genes) {
            if (!g.labSession) {
                String key = g.yearClass + "-" + g.course.getCode();
                placedHours.put(key, placedHours.getOrDefault(key, 0) + 1);
            }
        }

        // Subjects that are not yet full, sorted by most missing first
        // CHANGED: use correctHours() instead of getHoursPerWeek() throughout this block
        List<Course> retryCourses = theoryCourses.stream()
                .filter(c -> {
                    String key = c.getYearClass() + "-" + c.getCode();
                    int needed = correctHours(c);   // CHANGED
                    int placed = placedHours.getOrDefault(key, 0);
                    return placed < needed;
                })
                .sorted((a, b) -> {
                    int missA = correctHours(a) - placedHours.getOrDefault(a.getYearClass() + "-" + a.getCode(), 0); // CHANGED
                    int missB = correctHours(b) - placedHours.getOrDefault(b.getYearClass() + "-" + b.getCode(), 0); // CHANGED
                    return Integer.compare(missB, missA);
                })
                .collect(Collectors.toList());

        for (Course course : retryCourses) {
            String key = course.getYearClass() + "-" + course.getCode();
            int needed = correctHours(course);   // CHANGED
            int already = placedHours.getOrDefault(key, 0);
            int missing = needed - already;
            if (missing <= 0)
                continue;

            // Try multiple shuffles to find free slots
            for (int attempt = 0; attempt < 12 && missing > 0; attempt++) {
                List<Object[]> opts = theoryOptions(); // fresh shuffled list
                for (Object[] opt : opts) {
                    if (missing <= 0)
                        break;
                    TimetableSlot.SchoolDay day = (TimetableSlot.SchoolDay) opt[0];
                    int slot = (int) opt[1];
                    String code = course.getCode().toUpperCase();
                    boolean isOptional = code.contains("TPOBE") || code.contains("AUDBE") || code.contains("TGS")
                            || code.contains("TGSTE") || code.contains("PROJECT") || code.contains("AUDTE");
                    if (isOptional) {
                        if (day != TimetableSlot.SchoolDay.SAT)
                            continue;
                    } // else normal theory can use any day (including Saturday)

                    String ck = ck(day, slot, course.getYearClass(), null);
                    String fk = fk(day, slot, course.getFaculty().getId());
                    if (classUsed.contains(ck) || facultyUsed.contains(fk))
                        continue;

                    Room room = freeRoom(classrooms, day, slot, course.getYearClass(), roomUsed);
                    if (room == null)
                        continue;

                    Gene g = new Gene();
                    g.course = course;
                    g.faculty = course.getFaculty();
                    g.room = room;
                    g.day = day;
                    g.slotStart = slot;
                    g.slotEnd = slot;
                    g.yearClass = course.getYearClass();
                    g.division = null;
                    g.labSession = false;
                    chrom.genes.add(g);
                    classUsed.add(ck);
                    facultyUsed.add(fk);
                    roomUsed.add(rk(day, slot, room.getId()));
                    missing--;
                    placedHours.put(key, placedHours.get(key) + 1);
                }
            }
            if (missing > 0) {
                System.out.println("WARNING: Still missing " + missing + " hrs for " + course.getCode()
                        + " (needed " + needed + ", placed " + (already + (needed - missing)) + ")");
            }
        }

        return chrom;
    }

    // ── LAB PLACEMENT (unchanged, works for practicals) ─────────────────────
    private boolean placeLabBlockGroup(Chromosome chrom, List<Course> labs, Course.YearClass year, List<Room> sw,
            List<Room> lin, List<Room> proj, Set<String> classUsed, Set<String> facultyUsed, Set<String> roomUsed,
            Map<String, Integer> dayLabCount) {
        if (labs.size() < 3)
            return false;

        List<Course> shuffledLabs = new ArrayList<>(labs);
        Collections.shuffle(shuffledLabs, RNG);
        Course c1 = shuffledLabs.get(0);
        Course c2 = shuffledLabs.get(1);
        Course c3 = shuffledLabs.get(2);
        Course[] coursesArr = { c1, c2, c3 };
        String[] batches = { "I1", "I2", "I3" };

        Room[] rooms = pickLabRooms(sw, lin, proj);
        if (rooms == null)
            return false;

        int s1, s2;
        if (year == Course.YearClass.SE) {
            s1 = 1; s2 = 2;
        } else if (year == Course.YearClass.TE) {
            s1 = 3; s2 = 4;
        } else {
            s1 = 5; s2 = 6;
        }

        TimetableSlot.SchoolDay[] days = { TimetableSlot.SchoolDay.MON, TimetableSlot.SchoolDay.TUE,
                TimetableSlot.SchoolDay.WED, TimetableSlot.SchoolDay.THU, TimetableSlot.SchoolDay.FRI };

        for (TimetableSlot.SchoolDay day : days) {
            String countKey = year + "-" + day;
            if (dayLabCount.getOrDefault(countKey, 0) >= 1)
                continue;

            boolean ok = true;
            for (int i = 0; i < 3; i++) {
                Long fid = coursesArr[i].getFaculty().getId();
                if (facultyUsed.contains(fk(day, s1, fid)) || facultyUsed.contains(fk(day, s2, fid))
                        || roomUsed.contains(rk(day, s1, rooms[i].getId()))
                        || roomUsed.contains(rk(day, s2, rooms[i].getId()))
                        || classUsed.contains(ck(day, s1, year, batches[i]))
                        || classUsed.contains(ck(day, s2, year, batches[i]))) {
                    ok = false;
                    break;
                }
            }
            if (!ok)
                continue;

            for (int i = 0; i < 3; i++) {
                Gene g = new Gene();
                g.course = coursesArr[i];
                g.faculty = coursesArr[i].getFaculty();
                g.room = rooms[i];
                g.day = day;
                g.slotStart = s1;
                g.slotEnd = s2;
                g.yearClass = year;
                g.division = batches[i];
                g.labSession = true;
                chrom.genes.add(g);
                facultyUsed.add(fk(day, s1, coursesArr[i].getFaculty().getId()));
                facultyUsed.add(fk(day, s2, coursesArr[i].getFaculty().getId()));
                roomUsed.add(rk(day, s1, rooms[i].getId()));
                roomUsed.add(rk(day, s2, rooms[i].getId()));
                classUsed.add(ck(day, s1, year, batches[i]));
                classUsed.add(ck(day, s2, year, batches[i]));
            }
            dayLabCount.put(countKey, dayLabCount.getOrDefault(countKey, 0) + 1);
            return true;
        }
        return false;
    }

    private Room[] pickLabRooms(List<Room> sw, List<Room> lin, List<Room> proj) {
        List<List<Room>> pools = List.of(sw, lin, proj);
        Room[] result = new Room[3];
        for (int i = 0; i < 3; i++) {
            List<Room> pool = pools.get(i);
            if (!pool.isEmpty()) {
                result[i] = pool.get(RNG.nextInt(pool.size()));
                continue;
            }
            for (List<Room> p : pools) {
                if (!p.isEmpty()) {
                    result[i] = p.get(0);
                    break;
                }
            }
            if (result[i] == null)
                return null;
        }
        return result;
    }

    // ── THEORY PLACEMENT (two-pass spread, from theory.java) ────────────────
    private void placeTheory(Chromosome chrom, Course course,
            List<Room> classrooms,
            Set<String> classUsed,
            Set<String> facultyUsed,
            Set<String> roomUsed) {
int needed = course.getHoursPerWeek();
int placed = 0;
Set<TimetableSlot.SchoolDay> usedDays = new HashSet<>();

String code = course.getCode().toUpperCase();
boolean isOptional = code.contains("TPOBE") || code.contains("AUDBE") ||
            code.contains("TGS")    || code.contains("TGSTE") ||
            code.contains("PROJECT")|| code.contains("AUDTE");

List<Object[]> opts = theoryOptions(); // shuffled each call

for (int pass = 0; pass < 2 && placed < needed; pass++) {
for (Object[] opt : opts) {
if (placed >= needed) break;
TimetableSlot.SchoolDay day = (TimetableSlot.SchoolDay) opt[0];
int slot = (int) opt[1];

// Saturday slots beyond 4 are blocked by isBlocked() anyway (SAT_MAX=4)
if (day == TimetableSlot.SchoolDay.SAT && slot > 4) continue;

if (isOptional) {
   // Pass 0: only Saturday (preferred)
   if (pass == 0 && day != TimetableSlot.SchoolDay.SAT) continue;
   // Pass 1: only weekdays (Monday-Friday)
   if (pass == 1 && day == TimetableSlot.SchoolDay.SAT) continue;
} else {
   // Normal theory: no Saturday restriction now – all slots 1-4 allowed if needed.
   // However, still try to spread across different days in pass 0.
}

// Spread: avoid reusing the same day in pass 0 (optional courses also benefit)
if (pass == 0 && usedDays.contains(day)) continue;

String ck = ck(day, slot, course.getYearClass(), null);
String fk = fk(day, slot, course.getFaculty().getId());
if (classUsed.contains(ck) || facultyUsed.contains(fk)) continue;

Room room = freeRoom(classrooms, day, slot, course.getYearClass(), roomUsed);
if (room == null) continue;

Gene g = new Gene();
g.course = course;
g.faculty = course.getFaculty();
g.room = room;
g.day = day;
g.slotStart = slot;
g.slotEnd = slot;
g.yearClass = course.getYearClass();
g.division = null;
g.labSession = false;
chrom.genes.add(g);

classUsed.add(ck);
facultyUsed.add(fk);
roomUsed.add(rk(day, slot, room.getId()));
usedDays.add(day);
placed++;
}
}

if (placed < needed) {
System.out.println("NOT FULL -> " + course.getCode()
   + " Required=" + needed + " Placed=" + placed);
}
}    // ── Helper methods (unchanged) ─────────────────────────────────────────
    private Room freeRoom(List<Room> rooms, TimetableSlot.SchoolDay day, int slot, Course.YearClass year,
            Set<String> used) {
        List<Room> ordered = new ArrayList<>(rooms);
        ordered.sort((a, b) -> roomPref(b.getName(), year) - roomPref(a.getName(), year));
        for (Room r : ordered)
            if (!used.contains(rk(day, slot, r.getId())))
                return r;
        return null;
    }

    private int roomPref(String name, Course.YearClass y) {
        return switch (y) {
            case SE -> name.equals("1333") ? 2 : 1;
            case TE -> name.equals("1332") ? 2 : 1;
            case BE -> name.equals("1333") ? 2 : 1;
        };
    }

    // ── Hour-count correction for known mis-configured subjects ─────────────
    /**
     * Returns the correct weekly lecture hours for a theory course.
     * Overrides whatever is stored in the DB for subjects known to have
     * wrong hoursPerWeek values. Only theory subject hours are corrected here;
     * lab placement is completely untouched.
     *
     * Codes are matched EXACTLY (after trimming and uppercasing) against the
     * actual course codes visible in the deployed timetable, so there is no
     * risk of accidentally matching the wrong subject.
     *
     * Exact codes confirmed from timetable (Sem-II 2025-26):
     *   SE  → OE-II  (Project Management / SS)
     *   SE  → EVS    (Environmental Studies / AAG)
     *   SE  → PA     (Processor Architecture / SJ)
     *   TE  → CNS    (Computer Network & Security / ABG)
     *   TE  → ELECTIVE-II (Software Modelling & Design / DSH)
     */
    private int correctHours(Course course) {
        int stored = course.getHoursPerWeek();
        // Normalise: trim whitespace, collapse internal spaces, uppercase
        String code = course.getCode().trim().toUpperCase().replaceAll("\\s+", " ");
        Course.YearClass year = course.getYearClass();

        // ── Second Year (SE) corrections ──────────────────────────────────
        if (year == Course.YearClass.SE) {
            // OE-II → 2 hrs/week  (was stored as 0, so never placed at all)
            if (code.equals("OE-II"))
                return 2;
            // EVS → 2 hrs/week  (was stored as 0)
            if (code.equals("EVS"))
                return 2;
            // PA → 2 hrs/week  (was stored as 1, only got 1 slot)
            if (code.equals("PA"))
                return 2;
        }

        // ── Third Year (TE) corrections ───────────────────────────────────
        if (year == Course.YearClass.TE) {
            // CNS → 3 hrs/week  (was stored as 2)
            if (code.equals("CNS"))
                return 3;
            // ELECTIVE-II → 3 hrs/week  (was stored as 2)
            if (code.equals("ELECTIVE-II"))
                return 3;
        }

        // All other subjects: trust the stored value.
        // Guard: never return 0 — a subject with 0 stored hours would never be placed.
        return stored == 0 ? 1 : stored;
    }

    // ── Fitness, crossover, mutation, repair (original deepseek) ───────────
    public double evaluate(Chromosome chrom) {
        int hard = 0, soft = 0;
        int penalty = 0;

        Map<String, Integer> fSlot = new HashMap<>(), rSlot = new HashMap<>(), cSlot = new HashMap<>();
        Map<String, Integer> fDay = new HashMap<>();
        Map<String, Set<TimetableSlot.SchoolDay>> courseDays = new HashMap<>();
        Map<String, Map<String, Map<Integer, String>>> schedule = new HashMap<>();
        Map<String, Integer> subjectCount = new HashMap<>();
        Map<Long, Integer> facultyLoad = new HashMap<>();
        Map<String, Integer> labPerDay = new HashMap<>();

        for (Gene g : chrom.genes) {
            if (g.faculty == null || g.room == null) {
                hard += 10;
                continue;
            }

            int[] slots = g.labSession ? new int[] { g.slotStart, g.slotEnd } : new int[] { g.slotStart };
            for (int s : slots) {
                fSlot.merge(fk(g.day, s, g.faculty.getId()), 1, Integer::sum);
                rSlot.merge(rk(g.day, s, g.room.getId()), 1, Integer::sum);
                cSlot.merge(ck(g.day, s, g.yearClass, g.division), 1, Integer::sum);
                fDay.merge(g.day.name() + g.faculty.getId(), 1, Integer::sum);
                if (isBlocked(g.day, s)) hard += 5;
            }

            if (g.labSession) {
                String key = g.yearClass.name() + "-" + g.day.name();
                labPerDay.merge(key, 1, Integer::sum);
                if (g.room.getType() == Room.RoomType.CLASSROOM) hard += 5;
                if ((g.slotEnd - g.slotStart) != 1) hard += 8;
                if (g.day == TimetableSlot.SchoolDay.SAT && g.slotEnd > SAT_MAX) hard += 5;
                continue;
            }

            String key = g.yearClass + "-" + g.course.getCode();
            subjectCount.put(key, subjectCount.getOrDefault(key, 0) + 1);
            Long fid = g.faculty.getId();
            facultyLoad.put(fid, facultyLoad.getOrDefault(fid, 0) + 1);
            String k = g.yearClass.name() + g.course.getCode();
            if (!courseDays.computeIfAbsent(k, x -> new HashSet<>()).add(g.day)) soft += 3;
            String cls = g.yearClass + (g.division != null ? g.division : "");
            schedule.putIfAbsent(cls, new HashMap<>());
            Map<String, Map<Integer, String>> dayMap = schedule.get(cls);
            dayMap.putIfAbsent(g.day.name(), new HashMap<>());
            Map<Integer, String> slotMap = dayMap.get(g.day.name());
            if (slotMap.containsKey(g.slotStart - 1) && slotMap.get(g.slotStart - 1).equals(g.course.getCode()))
                penalty += 5;
            slotMap.put(g.slotStart, g.course.getCode());
        }

        for (int v : fSlot.values()) if (v > 1) hard += (v - 1) * 10;
        for (int v : rSlot.values()) if (v > 1) hard += (v - 1) * 10;
        for (int v : cSlot.values()) if (v > 1) hard += (v - 1) * 10;
        for (int v : fDay.values()) if (v > 5) soft += (v - 5) * 2;
        for (int v : labPerDay.values()) if (v > 1) hard += (v - 1) * 20;
        for (int count : subjectCount.values()) if (count > 2) penalty += (count - 2) * 2;
        for (int load : facultyLoad.values()) if (load > 4) penalty += (load - 4) * 2;

        // BE Lab batch subject enforcement (unchanged)
        Map<String, String> batchSubjectMap = new HashMap<>();
        batchSubjectMap.put("I1", "LPV");
        batchSubjectMap.put("I2", "LP-V");
        batchSubjectMap.put("I3", "LPVI");
        Map<String, Integer> batchCount = new HashMap<>();
        for (Gene g : chrom.genes) {
            if (g.labSession && g.yearClass == Course.YearClass.BE && g.day != TimetableSlot.SchoolDay.SAT) {
                String expected = batchSubjectMap.get(g.division);
                if (expected != null) {
                    if (!g.course.getCode().equalsIgnoreCase(expected)) hard += 100;
                    else batchCount.merge(g.division + "-" + expected, 1, Integer::sum);
                }
            }
        }
        for (String div : batchSubjectMap.keySet()) {
            String sub = batchSubjectMap.get(div);
            String key = div + "-" + sub;
            int count = batchCount.getOrDefault(key, 0);
            if (count < 2) hard += (2 - count) * 120;
            if (count > 2) hard += (count - 2) * 50;
        }

        // ========== NEW: Penalty for missing theory hours ==========
        Map<String, Integer> theoryPlaced = new HashMap<>();
        for (Gene g : chrom.genes) {
            if (!g.labSession && g.course != null) {
                String key = g.yearClass + "-" + g.course.getCode();
                theoryPlaced.put(key, theoryPlaced.getOrDefault(key, 0) + 1);
            }
        }
        for (Course c : allCourses) {
            if (c.getType() == Course.CourseType.LAB) continue;
            String key = c.getYearClass() + "-" + c.getCode();
            int placed = theoryPlaced.getOrDefault(key, 0);
            int needed = c.getHoursPerWeek();
            if (placed < needed) {
                penalty += (needed - placed) * 500;  // heavy penalty per missing hour
                System.out.println("MISSING HOURS: " + c.getCode() + " need " + needed + " got " + placed);
            }
        }

        // Final fitness: higher is better, max 10000
        return Math.max(0, 10000.0 - hard * 500.0 - soft - penalty);
    }
    public int countHard(Chromosome chrom) {
        if (chrom == null)
            return 9999;
        int hard = 0;
        Map<String, Integer> f = new HashMap<>(), r = new HashMap<>(), c = new HashMap<>();
        Map<String, Integer> labPerDay = new HashMap<>();
        for (Gene g : chrom.genes) {
            if (g.faculty == null || g.room == null) {
                hard++;
                continue;
            }
            int[] slots = g.labSession ? new int[] { g.slotStart, g.slotEnd } : new int[] { g.slotStart };
            for (int s : slots) {
                f.merge(fk(g.day, s, g.faculty.getId()), 1, Integer::sum);
                r.merge(rk(g.day, s, g.room.getId()), 1, Integer::sum);
                c.merge(ck(g.day, s, g.yearClass, g.division), 1, Integer::sum);
                if (isBlocked(g.day, s))
                    hard++;
            }
            if (g.labSession) {
                String key = g.yearClass.name() + "-" + g.day.name();
                labPerDay.merge(key, 1, Integer::sum);
            }
            if (g.labSession && g.room.getType() == Room.RoomType.CLASSROOM)
                hard++;
            if (!g.labSession && g.room.getType() != Room.RoomType.CLASSROOM)
                hard++;
            if (g.labSession && (g.slotEnd - g.slotStart) != 1)
                hard++;
        }
        for (int v : f.values())
            if (v > 1)
                hard += v - 1;
        for (int v : r.values())
            if (v > 1)
                hard += v - 1;
        for (int v : c.values())
            if (v > 1)
                hard += v - 1;
        for (int v : labPerDay.values())
            if (v > 1)
                hard += (v - 1);
        return hard;
    }

    public List<String> detectConflicts(Chromosome chrom) {
        List<String> conflicts = new ArrayList<>();
        Set<String> facultyUsed = new HashSet<>();
        Set<String> roomUsed = new HashSet<>();
        Set<String> classUsed = new HashSet<>();
        for (Gene g : chrom.genes) {
            int[] slots = g.labSession ? new int[] { g.slotStart, g.slotEnd } : new int[] { g.slotStart };
            for (int s : slots) {
                String fKey = g.day + "-" + s + "-F" + g.faculty.getId();
                String rKey = g.day + "-" + s + "-R" + g.room.getId();
                String cKey = g.day + "-" + s + "-" + g.yearClass + (g.division != null ? "-" + g.division : "");
                if (!facultyUsed.add(fKey))
                    conflicts.add("Faculty clash → " + g.faculty.getName() + " at " + g.day + " slot " + s);
                if (!roomUsed.add(rKey))
                    conflicts.add("Room clash → Room " + g.room.getName() + " at " + g.day + " slot " + s);
                if (!classUsed.add(cKey))
                    conflicts.add("Class clash → " + g.yearClass + " " + (g.division != null ? g.division : "") + " at "
                            + g.day + " slot " + s);
            }
        }
        return conflicts;
    }

    private void repair(Chromosome chrom, List<Room> classrooms, List<Room> sw, List<Room> lin, List<Room> proj) {
        Set<String> cu = new HashSet<>(), fu = new HashSet<>(), ru = new HashSet<>();
        List<Gene> ok = new ArrayList<>(), bad = new ArrayList<>();
        for (Gene g : chrom.genes) {
            if (g.faculty == null || g.room == null) {
                bad.add(g);
                continue;
            }
            int[] slots = g.labSession ? new int[] { g.slotStart, g.slotEnd } : new int[] { g.slotStart };
            boolean allOk = true;
            for (int s : slots) {
                if (fu.contains(fk(g.day, s, g.faculty.getId())) || ru.contains(rk(g.day, s, g.room.getId()))
                        || cu.contains(ck(g.day, s, g.yearClass, g.division))) {
                    allOk = false;
                    break;
                }
            }
            if (allOk) {
                ok.add(g);
                markUsed(g, fu, ru, cu);
            } else
                bad.add(g);
        }
        for (Gene g : bad) {
            if (g.faculty == null)
                continue;
            if (g.labSession) {
                Room room = labRoomForDivision(g.division, sw, lin, proj);
                if (room == null) {
                    ok.add(g);
                    continue;
                }
                boolean placed = false;
                for (Object[] opt : labOptions()) {
                    TimetableSlot.SchoolDay day = (TimetableSlot.SchoolDay) opt[0];
                    int s1 = (int) opt[1], s2 = (int) opt[2];
                    String fk1 = fk(day, s1, g.faculty.getId()), fk2 = fk(day, s2, g.faculty.getId());
                    String rk1 = rk(day, s1, room.getId()), rk2 = rk(day, s2, room.getId());
                    String ck1 = ck(day, s1, g.yearClass, g.division), ck2 = ck(day, s2, g.yearClass, g.division);
                    if (!fu.contains(fk1) && !fu.contains(fk2) && !ru.contains(rk1) && !ru.contains(rk2)
                            && !cu.contains(ck1) && !cu.contains(ck2)) {
                        g.day = day;
                        g.slotStart = s1;
                        g.slotEnd = s2;
                        g.room = room;
                        ok.add(g);
                        fu.add(fk1);
                        fu.add(fk2);
                        ru.add(rk1);
                        ru.add(rk2);
                        cu.add(ck1);
                        cu.add(ck2);
                        placed = true;
                        break;
                    }
                }
                if (!placed)
                    ok.add(g);
            } else {
                boolean placed = false;
                for (Object[] opt : theoryOptions()) {
                    TimetableSlot.SchoolDay day = (TimetableSlot.SchoolDay) opt[0];
                    int sl = (int) opt[1];
                    String fk = fk(day, sl, g.faculty.getId()), ck = ck(day, sl, g.yearClass, null);
                    if (!fu.contains(fk) && !cu.contains(ck)) {
                        Room room = freeRoom(classrooms, day, sl, g.yearClass, ru);
                        if (room == null)
                            continue;
                        g.day = day;
                        g.slotStart = sl;
                        g.slotEnd = sl;
                        g.room = room;
                        ok.add(g);
                        fu.add(fk);
                        cu.add(ck);
                        ru.add(rk(day, sl, room.getId()));
                        placed = true;
                        break;
                    }
                }
                if (!placed)
                    ok.add(g);
            }
        }
        chrom.genes = ok;
    }

    private void markUsed(Gene g, Set<String> fu, Set<String> ru, Set<String> cu) {
        int[] slots = g.labSession ? new int[] { g.slotStart, g.slotEnd } : new int[] { g.slotStart };
        for (int s : slots) {
            fu.add(fk(g.day, s, g.faculty.getId()));
            ru.add(rk(g.day, s, g.room.getId()));
            cu.add(ck(g.day, s, g.yearClass, g.division));
        }
    }

    private Room labRoomForDivision(String div, List<Room> sw, List<Room> lin, List<Room> proj) {
        List<Room> pool = switch (div != null ? div : "I1") {
            case "I1" -> sw.isEmpty() ? (lin.isEmpty() ? proj : lin) : sw;
            case "I2" -> lin.isEmpty() ? (proj.isEmpty() ? sw : proj) : lin;
            default -> proj.isEmpty() ? (sw.isEmpty() ? lin : sw) : proj;
        };
        return pool.isEmpty() ? null : pool.get(RNG.nextInt(pool.size()));
    }

    private void mutate(Chromosome chrom) {
        if (chrom.genes.isEmpty())
            return;
        Gene g = chrom.genes.get(RNG.nextInt(chrom.genes.size()));
        g.day = DAYS[RNG.nextInt(DAYS.length)];
        if (g.labSession) {
            int[] p = LAB_PAIRS[RNG.nextInt(LAB_PAIRS.length)];
            g.slotStart = p[0];
            g.slotEnd = p[1];
        } else {
            int s = THEORY_SLOTS[RNG.nextInt(THEORY_SLOTS.length)];
            g.slotStart = s;
            g.slotEnd = s;
        }
    }

    private Chromosome crossover(Chromosome p1, Chromosome p2) {
        Chromosome c = new Chromosome();
        int sz = Math.min(p1.genes.size(), p2.genes.size());
        int pt = sz > 1 ? RNG.nextInt(sz - 1) + 1 : 1;
        for (int i = 0; i < sz; i++)
            c.genes.add((i < pt ? p1 : p2).genes.get(i).deepCopy());
        List<Gene> lng = p1.genes.size() >= p2.genes.size() ? p1.genes : p2.genes;
        for (int i = sz; i < lng.size(); i++)
            c.genes.add(lng.get(i).deepCopy());
        return c;
    }

    private Chromosome ts(List<Chromosome> pop, int k) {
        Chromosome best = null;
        for (int i = 0; i < k; i++) {
            Chromosome c = pop.get(RNG.nextInt(pop.size()));
            if (best == null || c.fitness > best.fitness)
                best = c;
        }
        return best;
    }

    private List<Object[]> labOptions() {
        List<Object[]> opts = new ArrayList<>();
        for (TimetableSlot.SchoolDay day : DAYS)
            for (int[] pair : LAB_PAIRS)
                if (!isBlocked(day, pair[0]) && !isBlocked(day, pair[1]))
                    opts.add(new Object[] { day, pair[0], pair[1] });
        Collections.shuffle(opts, RNG);
        return opts;
    }

    private List<Object[]> theoryOptions() {
        List<Object[]> opts = new ArrayList<>();
        for (TimetableSlot.SchoolDay day : DAYS)
            for (int s : THEORY_SLOTS)
                if (!isBlocked(day, s))
                    opts.add(new Object[] { day, s });
        Collections.shuffle(opts, RNG);
        return opts;
    }

    private String fk(TimetableSlot.SchoolDay d, int s, Long fid) {
        return d.name() + "-" + s + "-F" + fid;
    }

    private String rk(TimetableSlot.SchoolDay d, int s, Long rid) {
        return d.name() + "-" + s + "-R" + rid;
    }

    private String ck(TimetableSlot.SchoolDay d, int s, Course.YearClass y, String div) {
        return d.name() + "-" + s + "-" + y.name() + (div != null ? "-" + div : "");
    }

    private boolean isBlocked(TimetableSlot.SchoolDay d, int s) {
        return BLOCKED.contains(d.name() + "-" + s) || (d == TimetableSlot.SchoolDay.SAT && s > SAT_MAX);
    }

    private List<Room> byType(List<Room> rooms, Room.RoomType t) {
        return rooms.stream().filter(r -> r.getType() == t).collect(Collectors.toList());
    }

    // ── Inner classes ──────────────────────────────────────────────────────
    public static class Chromosome {
        public List<Gene> genes = new ArrayList<>();
        public double fitness = 0;
        public Chromosome deepCopy() {
            Chromosome c = new Chromosome();
            c.fitness = this.fitness;
            for (Gene g : genes)
                c.genes.add(g.deepCopy());
            return c;
        }
    }

    public static class Gene {
        public Course course;
        public Faculty faculty;
        public Room room;
        public TimetableSlot.SchoolDay day;
        public int slotStart;
        public int slotEnd;
        public Course.YearClass yearClass;
        public String division;
        public boolean labSession;
        public Gene deepCopy() {
            Gene g = new Gene();
            g.course = course;
            g.faculty = faculty;
            g.room = room;
            g.day = day;
            g.slotStart = slotStart;
            g.slotEnd = slotEnd;
            g.yearClass = yearClass;
            g.division = division;
            g.labSession = labSession;
            return g;
        }
    }

    public static class GaConfig {
        public int populationSize = 200;
        public int maxGenerations = 1000;
        public double mutationRate = 0.08;
        public double crossoverRate = 0.85;
        public int eliteCount = 10;
        public int tournamentSize = 7;
    }

    public static class GaResult {
        public final Chromosome best;
        public final int generationsRun;
        public final List<Double> fitnessHistory;
        public GaResult(Chromosome b, int g, List<Double> h) {
            best = b;
            generationsRun = g;
            fitnessHistory = h;
        }
    }
}