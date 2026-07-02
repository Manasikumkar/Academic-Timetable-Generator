package com.itdept.timetable.controller;

import com.itdept.timetable.model.*;
import com.itdept.timetable.service.ConflictDetectionService;
import com.itdept.timetable.service.ConflictDetectionService.ConflictInfo;
import com.itdept.timetable.service.TimetableGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/timetable")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableGenerationService generationService;
    private final ConflictDetectionService   conflictService;

    @GetMapping("/versions")
    public ResponseEntity<List<TimetableVersion>> getAllVersions() {
        return ResponseEntity.ok(generationService.getAllVersions());
    }

    @GetMapping("/versions/{id}")
    public ResponseEntity<TimetableVersion> getVersion(@PathVariable Long id) {
        return ResponseEntity.ok(generationService.getVersion(id));
    }

    /**
     * POST /api/timetable/generate
     * Body: {
     *   name, academicYear,
     *   semester: 1 or 2,          ← NEW: 1=Odd, 2=Even
     *   years: "SE,TE,BE",         ← NEW: which years to schedule
     *   populationSize?, maxGenerations?, mutationRate?
     * }
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String,Object>> generate(@RequestBody Map<String,Object> body) {
        String name         = (String) body.getOrDefault("name", "Sem-II 2025-26");
        String academicYear = (String) body.getOrDefault("academicYear", "2025-26");
        int    semester     = body.containsKey("semester")
                             ? Integer.parseInt(body.get("semester").toString()) : 2;
        String years        = (String) body.getOrDefault("years", "SE,TE,BE");

        Integer popOverride = body.containsKey("populationSize")
                             ? Integer.parseInt(body.get("populationSize").toString()) : null;
        Integer genOverride = body.containsKey("maxGenerations")
                             ? Integer.parseInt(body.get("maxGenerations").toString()) : null;
        Double  mutOverride = body.containsKey("mutationRate")
                             ? Double.parseDouble(body.get("mutationRate").toString()) : null;

        TimetableVersion version = generationService.generate(
            name, academicYear, semester, years, popOverride, genOverride, mutOverride);

        Map<String,Object> resp = new HashMap<>();
        resp.put("version", version);
        resp.put("message", version.getHardConflicts() == 0
            ? "Conflict-free timetable generated successfully!"
            : version.getHardConflicts() + " conflict(s) remain. Try regenerating with higher population.");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{versionId}/slots")
    public ResponseEntity<List<TimetableSlot>> getSlots(
            @PathVariable Long versionId,
            @RequestParam(required = false) String yearClass) {
        Course.YearClass yc = yearClass != null ? Course.YearClass.valueOf(yearClass.toUpperCase()) : null;
        return ResponseEntity.ok(generationService.getSlots(versionId, yc));
    }

    @GetMapping("/{versionId}/conflicts")
    public ResponseEntity<Map<String,Object>> getConflicts(@PathVariable Long versionId) {
        List<ConflictInfo> conflicts = conflictService.detect(versionId);
        Map<String,Object> resp = new HashMap<>();
        resp.put("versionId", versionId);
        resp.put("totalConflicts", conflicts.size());
        resp.put("conflicts", conflicts);
        return ResponseEntity.ok(resp);
    }

    @PatchMapping("/{versionId}/finalize")
    public ResponseEntity<TimetableVersion> finalize(@PathVariable Long versionId) {
        return ResponseEntity.ok(generationService.updateStatus(versionId, TimetableVersion.Status.FINAL));
    }

    @PatchMapping("/{versionId}/deploy")
    public ResponseEntity<TimetableVersion> deploy(@PathVariable Long versionId) {
        return ResponseEntity.ok(generationService.updateStatus(versionId, TimetableVersion.Status.DEPLOYED));
    }

    @DeleteMapping("/{versionId}")
    public ResponseEntity<Void> deleteVersion(@PathVariable Long versionId) {
        generationService.deleteVersion(versionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/active")
    public ResponseEntity<Map<String,Object>> getActive(@RequestParam(required = false) String yearClass) {
        List<TimetableVersion> deployed = generationService.getAllVersions()
            .stream().filter(v -> v.getStatus() == TimetableVersion.Status.DEPLOYED).toList();
        if (deployed.isEmpty()) return ResponseEntity.ok(Map.of("message","No timetable deployed yet."));
        TimetableVersion active = deployed.get(0);
        Course.YearClass yc = yearClass != null ? Course.YearClass.valueOf(yearClass.toUpperCase()) : null;
        return ResponseEntity.ok(Map.of("version", active, "slots", generationService.getSlots(active.getId(), yc)));
    }
}