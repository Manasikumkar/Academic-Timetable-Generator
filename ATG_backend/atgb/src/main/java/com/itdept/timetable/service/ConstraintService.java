package com.itdept.timetable.service;

import com.itdept.timetable.model.Constraint;
import com.itdept.timetable.repository.ConstraintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConstraintService {

    private final ConstraintRepository constraintRepository;

    public List<Constraint> getAll() {
        return constraintRepository.findAll();
    }

    public List<Constraint> getActive() {
        return constraintRepository.findByActiveTrue();
    }

    @Transactional
    public Constraint create(Constraint.ConstraintType type, String name,
                             String description, int penalty, boolean active) {
        return constraintRepository.save(Constraint.builder()
                .type(type)
                .name(name)
                .description(description)
                .penalty(penalty)
                .active(active)
                .build());
    }

    @Transactional
    public Constraint toggleActive(Long id) {
        Constraint c = constraintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Constraint not found: " + id));
        c.setActive(!c.isActive());
        return constraintRepository.save(c);
    }

    @Transactional
    public void delete(Long id) {
        constraintRepository.deleteById(id);
    }

    /**
     * Seeds the default constraints based on your IT department timetable rules.
     * Call once on startup via DataInitializer if the table is empty.
     */
    @Transactional
    public void seedDefaults() {
        if (constraintRepository.count() > 0) return;

        // ── HARD constraints ─────────────────────────────────────────────
        List.of(
            new String[]{"No faculty clash",
                "A faculty member cannot be assigned to more than one class in the same time slot.", "100"},
            new String[]{"No room clash",
                "A room cannot be used by more than one class in the same time slot.", "100"},
            new String[]{"No class double-booking",
                "A class (SE/TE/BE) cannot have two subjects scheduled simultaneously.", "100"},
            new String[]{"Correct room type",
                "Lab sessions must be held in a lab; theory sessions must be in a classroom.", "80"},
            new String[]{"Faculty availability",
                "Faculty cannot be scheduled during their marked unavailable slots.", "100"}
        ).forEach(a -> create(Constraint.ConstraintType.HARD,
                a[0], a[1], Integer.parseInt(a[2]), true));

        // ── SOFT constraints ─────────────────────────────────────────────
        List.of(
            new String[]{"Spread theory sessions",
                "The same theory course should not be scheduled on the same day more than once.", "3"},
            new String[]{"Limit faculty daily load",
                "A faculty member should not teach more than 4 sessions in a single day.", "5"},
            new String[]{"Labs in dedicated slots",
                "Lab batches should be scheduled in slots 3–6 (post short-break / afternoon).", "2"},
            new String[]{"Avoid Saturday for seniors",
                "BE classes should preferably not be scheduled on Saturday.", "4"},
            new String[]{"TPO slot protection",
                "TPO sessions should be kept separate and not clash with core subjects.", "5"}
        ).forEach(a -> create(Constraint.ConstraintType.SOFT,
                a[0], a[1], Integer.parseInt(a[2]), true));
    }
}