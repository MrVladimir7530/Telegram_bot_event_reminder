package com.example.telegramboteventteminder.service;


import com.example.telegramboteventteminder.config.BotConfig;
import com.example.telegramboteventteminder.model.User;
import com.example.telegramboteventteminder.model.UserReminder;
import com.example.telegramboteventteminder.repository.ReminderRepository;
import com.example.telegramboteventteminder.repository.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReminderRepository reminderRepository;
    private final BotConfig CONFIG;
    private static final String HELP_TEXT = "This bot has command:\n" +
            "\"/start\", \"get a welcome message\"\n" +
            "\"/add\", \"adds a new reminder\"\n" +
            "\"/delete\", \"deletes a current reminder\"\n" +
            "\"/getAll\", \"gets all reminders\n" +
            "\"/help\", \"gives help on this bot\"\n" +
            "\"/info\", \"gives information about this bot\"";
    private static final String INFO_TEXT = "This bot is needed to remind you of various events.\n" +
            "To get started you need to write \"start\". In this\n" +
            "bot you can create and also delete reminders. You can\n" +
            "use ready-made commands from the menu at the bottom right";
    static final String ERROR_TEXT = "Error occurred: ";
    private static String YES_BUTTON = "YES_BUTTON";
    private static String NO_BUTTON = "NO_BUTTON";
    private int chooseWay = 0;
    private LocalDateTime localDateTime = LocalDateTime.now();

    public TelegramBot(BotConfig config) {
        CONFIG = config;
        List<BotCommand> listOfCommand = new ArrayList<>();
        listOfCommand.add(new BotCommand("/start", "get a welcome message"));
        listOfCommand.add(new BotCommand("/add", "adds a new reminder"));
        listOfCommand.add(new BotCommand("/delete", "deletes a current reminder"));
        listOfCommand.add(new BotCommand("/getAll", "gets all reminders"));
        listOfCommand.add(new BotCommand("/help", "gives help on this bot"));
        listOfCommand.add(new BotCommand("/info", "gives information about this bot"));
        try {
            this.execute(new SetMyCommands(listOfCommand, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return CONFIG.getBotName();
    }

    @Override
    public String getBotToken() {
        return CONFIG.getToken();
    }


    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            if (update.getMessage().getText().equals("/cancel")) {
                //todo
                chooseWay = 0;
            } else {
                switch (chooseWay) {
                    case 0:
                        startMenu(update);
                        break;
                    case 1:
                        chooseDataWithTime(update);
                        break;
                    case 2:
                        saveEvent(update, localDateTime);
                        break;
                    case 3:
                        deleteEvent(update);
                        break;
                }
            }
        } else {
            prepareAndSendMessage(update.getMessage().getChatId(), "select command");
        }
    }

    private void startMenu(Update update) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        switch (messageText) {
            case "/start":
                registerUser(update.getMessage());
                startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                break;
            case "/add":
                chooseWay = 1;
                prepareAndSendMessage(chatId, "Write the date with time or the time of the event");
                break;
            case "/delete":
                chooseWay = 3;
                getAll(update);
                prepareAndSendMessage(chatId, "Write Number \"chat_id\", which needs to be deleted");
                break;
            case "/getAll":
                getAll(update);
                break;
            case "/help":
                prepareAndSendMessage(chatId, HELP_TEXT);
                break;
            case "/info":
                prepareAndSendMessage(chatId, INFO_TEXT);
                break;
            default:
                prepareAndSendMessage(chatId, "Sorry, Bro, this command isn't support " + ":pensive:");

        }
    }

    private void chooseDataWithTime(Update update) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        try {
            if (messageText.replaceAll("\\s", "").length() == 5) {
                LocalTime localTime = LocalTime.parse(messageText);
                localDateTime = LocalDateTime.of(LocalDate.now(), localTime);
            } else {
                localDateTime = LocalDateTime.parse(messageText);
            }
            chooseWay = 2;
            prepareAndSendMessage(chatId, "Date: " + localDateTime + ", now enter message");
        } catch (Exception e) {
            log.info("Incorrect data entered: " + e.getMessage());
            prepareAndSendMessage(chatId, "Incorrect data entered, please try again");
        }
    }

    private void saveEvent(Update update, LocalDateTime localDateTime) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        UserReminder reminder = new UserReminder();
        reminder.setMessageReminder(messageText);
        reminder.setDataReminder(localDateTime);
        User user = userRepository.findById(chatId).get();
        reminder.setUser(user);
        try {
            reminderRepository.save(reminder);
            chooseWay = 0;
            prepareAndSendMessage(chatId, "reminder: " + messageText + " successfully added");
        } catch (Exception e) {
            log.error("This reminder is not saved: " + e.getMessage());
        }
    }

    private void deleteEvent(Update update) {
        String text = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        try {
            Long idReminder = Long.valueOf(text);
            reminderRepository.deleteById(idReminder);
            chooseWay = 0;
            prepareAndSendMessage(chatId, "This reminder was deleted successfully");
        } catch (Exception e) {
            log.info("Incorrect data entered" + e.getMessage());
            prepareAndSendMessage(chatId, "Incorrect data entered, please try again");
        }
    }

    private void getAll(Update update) {
        long chatId = update.getMessage().getChatId();

        List<UserReminder> collect = reminderRepository.findAll()
                .stream()
                .collect(Collectors.toList());
        if (collect.isEmpty()) {
            prepareAndSendMessage(chatId, "No reminders");
        }
        for (UserReminder userReminder : collect) {
            String textToSend = "Id=" + userReminder.getId()
                    + ", message=" + userReminder.getMessageReminder()
                    + ", data=" + userReminder.getDataReminder()
                    + "\n";
            prepareAndSendMessage(chatId, textToSend);
        }

    }


    private void startCommandReceived(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Hi, " + name + ", nice to meet you!" + " :blush:");
        log.info("Replied to user " + name);
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("/add");
        row.add("/delete");
        row.add("/getAll");
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("/cancel");
        row.add("/info");
        row.add("/help");
        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);

        message.setReplyMarkup(keyboardMarkup);
        executeMessage(message);
    }

    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }


    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void registerUser(Message message) {
        if (userRepository.findById(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUsername(chat.getUserName());
            user.setRegisteredAt(LocalDateTime.now());

            userRepository.save(user);
            log.info("user saved: " + user);
        }
    }

    @Scheduled(cron = "0 * * * * *")
    private void checkReminderAndSendToUser() {
        List<UserReminder> all = reminderRepository.findAll();
        for (UserReminder userReminder : all) {
            if (userReminder.getDataReminder().truncatedTo(ChronoUnit.MINUTES).equals(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))) {
                Long chatId = userReminder.getUser().getChatId();
                prepareAndSendMessage(chatId, "reminder: " + userReminder.getMessageReminder());
                reminderRepository.deleteById(userReminder.getId());
            }
        }
    }

}
