package com.itdept.timetable.controller;

import com.itdept.timetable.model.Room;
import com.itdept.timetable.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<List<Room>> getAll(
            @RequestParam(required = false) String type) {
        if (type != null) {
            return ResponseEntity.ok(
                    roomService.getByType(Room.RoomType.valueOf(type.toUpperCase())));
        }
        return ResponseEntity.ok(roomService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> getById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Room> create(@RequestBody Map<String, Object> body) {
        Room saved = roomService.create(
                (String) body.get("name"),
                Room.RoomType.valueOf((String) body.get("type")),
                body.containsKey("capacity") ? (int) body.get("capacity") : 60
        );
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Room> update(@PathVariable Long id,
                                       @RequestBody Map<String, Object> body) {
        Room saved = roomService.update(
                id,
                (String) body.get("name"),
                Room.RoomType.valueOf((String) body.get("type")),
                body.containsKey("capacity") ? (int) body.get("capacity") : 60
        );
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return ResponseEntity.noContent().build();
    }
}