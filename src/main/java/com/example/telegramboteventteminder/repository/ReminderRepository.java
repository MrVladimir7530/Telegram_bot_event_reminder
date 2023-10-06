package com.example.telegramboteventteminder.repository;

import com.example.telegramboteventteminder.model.UserReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReminderRepository extends JpaRepository<UserReminder, Long> {
    List<UserReminder> findAllByDataReminder(LocalDateTime localDateTime);
    List<UserReminder> findAllByDataReminderIsBefore(LocalDateTime localDateTime);
}
