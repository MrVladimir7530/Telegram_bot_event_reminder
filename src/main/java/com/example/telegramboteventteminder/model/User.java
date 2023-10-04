package com.example.telegramboteventteminder.model;

import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Getter
@Setter
@ToString
@Entity(name = "usersDataTable")
public class User {
    @Id
    private Long chatId;
    private String firstName;
    private String lastName;
    private String username;
    private Timestamp registeredAt;
    @OneToMany
    @JoinColumn(name = "user")
    private List<UserReminder> userReminder;

}
