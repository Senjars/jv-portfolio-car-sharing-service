package com.github.senjars.carsharing.notify;

import com.github.senjars.carsharing.config.TelegramConfig;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final TelegramConfig telegramConfig;

    public TelegramBot(TelegramConfig telegramConfig) {
        this.telegramConfig = telegramConfig;
    }

    @Override
    public void onUpdateReceived(Update update) {
    }

    @Override
    public String getBotUsername() {
        return telegramConfig.getBotUsername();
    }

    @Override
    public String getBotToken() {
        return telegramConfig.getBotToken();
    }
}
