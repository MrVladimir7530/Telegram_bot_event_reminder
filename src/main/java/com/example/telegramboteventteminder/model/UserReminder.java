package com.example.telegramboteventteminder.model;

import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;

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
    private Timestamp dataReminder;
    @ManyToOne
    @JoinColumn(name = "userReminder")
    private User user;
}
