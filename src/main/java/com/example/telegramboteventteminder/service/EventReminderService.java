package com.example.telegramboteventteminder.service;

import com.example.telegramboteventteminder.model.User;
import com.example.telegramboteventteminder.model.UserReminder;
import com.example.telegramboteventteminder.repository.ReminderRepository;
import com.example.telegramboteventteminder.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class EventReminderService {
    @Autowired
    private ReminderRepository reminderRepository;
//    @Autowired
//    private UserRepository userRepository;

    public void saveNewReminder(User user) {
        Long chatId = user.getChatId();
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Enter reminder date");


    }

    public void checkReminder() {

    }



}

