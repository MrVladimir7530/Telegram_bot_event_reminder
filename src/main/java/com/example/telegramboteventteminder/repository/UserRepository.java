package com.example.telegramboteventteminder.repository;


import com.example.telegramboteventteminder.model.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
}
