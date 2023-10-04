package com.example.telegramboteventteminder.repository;

import com.example.telegramboteventteminder.model.UserReminder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<UserReminder, Long> {
}
