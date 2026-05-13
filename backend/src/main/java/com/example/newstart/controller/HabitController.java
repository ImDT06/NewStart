package com.example.newstart.controller;

import com.example.newstart.entity.Habit;
import com.example.newstart.entity.User;
import com.example.newstart.repository.HabitRepository;
import com.example.newstart.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/habits")
public class HabitController {
    private final HabitRepository habitRepository;
    private final UserRepository userRepository;

    public HabitController(HabitRepository habitRepository, UserRepository userRepository) {
        this.habitRepository = habitRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Habit> getHabits(@RequestParam String userId) {
        return habitRepository.findByUserId(userId);
    }

    @PostMapping
    public Habit createHabit(@RequestParam String userId, @RequestBody Habit habit) {
        User user = userRepository.findById(userId).orElseGet(() -> {
            User newUser = new User();
            newUser.setId(userId);
            newUser.setName("NewStart User");
            newUser.setEmail(userId + "@newstart.local");
            return userRepository.save(newUser);
        });
        habit.setUser(user);
        return habitRepository.save(habit);
    }

    @PutMapping("/{id}/toggle")
    public Habit toggleHabit(@PathVariable Long id) {
        Habit habit = habitRepository.findById(id).orElseThrow();
        habit.setDone(!habit.isDone());
        if (habit.isDone()) {
            habit.setStreak(habit.getStreak() + 1);
        }
        return habitRepository.save(habit);
    }
}
