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

import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

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
            "\"/info\", \"gives information about this bot\"\n" +
            "\"/cancel\", \"cancels the action and returns to the main menu\"";
    private static final String INFO_TEXT = "This bot is needed to remind you of various events.\n" +
            "To get started you need to write \"/start\". In this\n" +
            "bot you can create and also delete reminders. You can\n" +
            "use ready-made commands from the menu at the bottom right";
    static final String ERROR_TEXT = "Error occurred: ";
    private static final String YES_BUTTON = "YES_BUTTON";
    private static final String NO_BUTTON = "NO_BUTTON";
    private int chooseWay = 0;
    private LocalDateTime localDateTime = LocalDateTime.now();

    public TelegramBot(BotConfig config) {
        CONFIG = config;
        List<BotCommand> listOfCommand = new ArrayList<>();
        listOfCommand.add(new BotCommand("/start", "get a welcome message"));
        listOfCommand.add(new BotCommand("/add", "adds a new reminder"));
        listOfCommand.add(new BotCommand("/delete", "deletes a current reminder"));
        listOfCommand.add(new BotCommand("/getAll", "gets all reminders"));
        listOfCommand.add(new BotCommand("/info", "gives information about this bot"));
        listOfCommand.add(new BotCommand("/help", "gives help on this bot"));
        try {
            this.execute(new SetMyCommands(listOfCommand, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list " + e.getMessage());
        }
    }

    @PostConstruct
    public void init() {
        checkReminderThatHavePassed();
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
                confirmCancellation(update);
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
        } else if (update.hasCallbackQuery()) {
            String callBackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            if (callBackData.equals(YES_BUTTON)) {
                chooseWay = 0;
                EditMessageText messageText = new EditMessageText();
                messageText.setChatId(String.valueOf(chatId));
                messageText.setText(INFO_TEXT);
                messageText.setMessageId((int) messageId);
                executeEditMessage(messageText);
            } else if (callBackData.equals(NO_BUTTON)) {
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(String.valueOf(chatId));
                deleteMessage.setMessageId((int) messageId);
                try {
                    execute(deleteMessage);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }

        } else {
            prepareAndSendMessage(update.getMessage().getChatId(), "select command");
        }
    }

    private void executeEditMessage(EditMessageText messageText) {
        try {
            execute(messageText);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
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
                prepareAndSendMessage(chatId, "Write the date with time or the time of the event.\n" +
                        "In format: \"yyyy\\mm\\dd\\ hh:mm\" or \"hh:mm\"\n" +
                        "For example: 2024\\02\\15 15:21 or 15:21");
                break;
            case "/delete":
                if (getAll(update).isEmpty()) {
                    break;
                }
                chooseWay = 3;
                prepareAndSendMessage(chatId, "Write Number \"Id\", which needs to be deleted");
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
        messageText = messageText.trim();
        long chatId = update.getMessage().getChatId();
        try {
            if (messageText.length() <= 5) {
                if (messageText.length() == 4) {
                    messageText = "0" + messageText;
                }
            StringBuilder stringBuilder = new StringBuilder(messageText);
                stringBuilder.replace(2, 3, ":");
                LocalTime localTime = LocalTime.parse(stringBuilder);
                localDateTime = LocalDateTime.of(LocalDate.now(), localTime);
            } else {
            StringBuilder stringBuilder = new StringBuilder(messageText);
            stringBuilder.replace(4, 5, "-")
                    .replace(7,8,"-")
                    .replace(10,11, "T")
                    .replace(13,14,":");
                localDateTime = LocalDateTime.parse(stringBuilder);
            }
            LocalDateTime localDateTimeNow = LocalDateTime.now();
            if (localDateTimeNow.truncatedTo(ChronoUnit.MINUTES).isAfter(localDateTime.truncatedTo(ChronoUnit.MINUTES))) {
                prepareAndSendMessage(chatId, "This date has already passed");
                return;
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
            prepareAndSendMessage(chatId, "reminder: \"" + messageText + "\" successfully added");
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

    private List<?extends UserReminder> getAll(Update update) {
        long chatId = update.getMessage().getChatId();

        List<UserReminder> collect = new ArrayList<>(reminderRepository.findByUser_ChatId(chatId));
        if (collect.isEmpty()) {
            prepareAndSendMessage(chatId, "No reminders");
            return collect;
        }
        for (UserReminder userReminder : collect) {
            String textToSend = "(Id=" + userReminder.getId() + ") "
                    + userReminder.getDataReminder()
                    + "  " + userReminder.getMessageReminder()
                    + "\n";
                prepareAndSendMessage(chatId, textToSend);
        }
        return collect;
    }


    private void startCommandReceived(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Hi, " + name + ", Welcome. To add a new reminder write \"/add\"!" + " :blush:");
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

    private void confirmCancellation(Update update) {
        Long chatId = update.getMessage().getChatId();
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Are you sure you want to cancel the action?" + "");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        var yesButton = new InlineKeyboardButton();

        yesButton.setText("Yes");
        yesButton.setCallbackData(YES_BUTTON);

        var noButton = new InlineKeyboardButton();

        noButton.setText("No");
        noButton.setCallbackData(NO_BUTTON);

        rowInLine.add(yesButton);
        rowInLine.add(noButton);
        rowsInLine.add(rowInLine);
        markup.setKeyboard(rowsInLine);
        message.setReplyMarkup(markup);

        executeMessage(message);
    }

    @Scheduled(cron = "0 * * * * *")
    private void checkReminderAndSendToUser() {
        List<UserReminder> allByDataReminder = reminderRepository.findAllByDataReminder(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        for (UserReminder userReminder : allByDataReminder) {
            Long chatId = userReminder.getUser().getChatId();
            prepareAndSendMessage(chatId, "reminder: " + userReminder.getMessageReminder());
            reminderRepository.deleteById(userReminder.getId());
        }
    }

    private void checkReminderThatHavePassed() {
        List<UserReminder> allByDataReminderIsBefore = reminderRepository.findAllByDataReminderIsBefore(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        for (UserReminder userReminder : allByDataReminderIsBefore) {
            Long chatId = userReminder.getUser().getChatId();
            prepareAndSendMessage(chatId, "reminder: " + userReminder.getMessageReminder());
            reminderRepository.deleteById(userReminder.getId());
        }
    }


}
