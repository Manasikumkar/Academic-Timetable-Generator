package com.itdept.timetable.service;

import com.itdept.timetable.model.Room;
import com.itdept.timetable.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    public List<Room> getAll() {
        return roomRepository.findAll();
    }

    public List<Room> getByType(Room.RoomType type) {
        return roomRepository.findByType(type);
    }

    public Room getById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found: " + id));
    }

    @Transactional
    public Room create(String name, Room.RoomType type, int capacity) {
        Room room = Room.builder()
                .name(name)
                .type(type)
                .capacity(capacity)
                .build();
        return roomRepository.save(room);
    }

    @Transactional
    public Room update(Long id, String name, Room.RoomType type, int capacity) {
        Room room = getById(id);
        room.setName(name);
        room.setType(type);
        room.setCapacity(capacity);
        return roomRepository.save(room);
    }

    @Transactional
    public void delete(Long id) {
        roomRepository.deleteById(id);
    }
}