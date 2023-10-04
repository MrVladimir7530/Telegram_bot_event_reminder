package com.example.telegramboteventteminder.repository;


import com.example.telegramboteventteminder.model.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, Long> {
}
