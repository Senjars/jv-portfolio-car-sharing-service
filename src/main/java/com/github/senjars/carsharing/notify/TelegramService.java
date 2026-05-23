package com.github.senjars.carsharing.notify;

import com.github.senjars.carsharing.config.TelegramConfig;
import com.github.senjars.carsharing.exception.TelegramNotificationException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
public class TelegramService {

    private final TelegramBot telegramBot;
    private final TelegramConfig telegramConfig;

    @Async
    public void sendMessage(String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(telegramConfig.getChatId());
        sendMessage.setText(message);

        try {
            telegramBot.execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new TelegramNotificationException("Failed to send message to Telegram: "
                    + e.getMessage());
        }
    }
}
