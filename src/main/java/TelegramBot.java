import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramBot extends TelegramLongPollingBot {
    private final Function<String, String> messageProcessor;
    private final Logger logger = LogManager.getLogger(getClass());
    private final List<MessagePattern> messagePatterns = new ArrayList<>();
    private final String botName;
    private final String token;

    public TelegramBot(String name, String token, DefaultBotOptions options, Function<String, String> messageProcessor) {
        super(options);
        this.botName = name;
        this.token = token;
        this.messageProcessor = messageProcessor;
    }

    public MessagePattern addMatcher(String regexp) {
        MessagePattern pattern = new MessagePattern(regexp);
        messagePatterns.add(pattern);
        return pattern;
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (message == null || !message.hasText()) {
            return;
        }
        String text = message.getText();
        logger.debug("got message: " + text);
        for (MessagePattern pattern : messagePatterns) {
            if (pattern.messagePattern.matcher(text).find()) {
                String filtered;
                if (pattern.target == null) {
                    filtered = text;
                } else {
                    filtered = text.replace(pattern.target, pattern.replacement);
                }
                String answer = messageProcessor.apply(filtered);
                try {
                    sendMsg(message.getChatId(), answer);
                } catch (TelegramApiException e) {
                    logger.error(e.getMessage(), e);
                }
                return;
            }
        }
        try {
            sendMsg(message.getChatId(), "chat id: " + update.getMessage().getChatId() + ": put your youtube link here.");
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    public void sendMsg(Long chatId, String text) throws TelegramApiException {
        SendMessage s = new SendMessage();
        s.enableMarkdown(true);
        s.setChatId(chatId);
        s.setText(text);
        execute(s);
    }

    static class MessagePattern {
        private final Pattern messagePattern;
        private String target;
        private String replacement;

        public MessagePattern(final String regexp) {
            this.messagePattern = Pattern.compile(regexp);
        }

        public void addReplacement(String target, String replacement) {
            this.target = target;
            this.replacement = replacement;
        }
    }
}
