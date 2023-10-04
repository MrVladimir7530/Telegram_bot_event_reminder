package com.example.telegramboteventteminder.model;

import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Entity(name = "userReminder")
public class UserReminder {
    @Id
    @GeneratedValue
    private Long id;
    private String messageReminder;
    private LocalDateTime dataReminder;
    @ManyToOne
    @JoinColumn(name = "user_chat_id")
    private User user;
}
