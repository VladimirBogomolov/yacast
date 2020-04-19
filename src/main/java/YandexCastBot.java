import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.ApiContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

public class YandexCastBot {
    private static final Logger logger = LogManager.getLogger(YandexCastBot.class);

    public static void main(String[] args) throws InterruptedException, TelegramApiRequestException {
        YaCastConfiguration config;
        if (args.length > 0) {
            String param1 = args[0];
            Path configPath = Paths.get(param1);
            config = new YaCastConfiguration(configPath);
            logger.info("Config path " + configPath);
        } else {
            config = new YaCastConfiguration();
        }
        YandexCastClient yandexCastClient = new YandexCastClient(config.getYaLogin(), config.getYaPasswd())
            .withDeviceId(config.getYaDeviceId());
        ApiContextInitializer.init();
        TelegramBot bot = createTelegramBot(config.getTelegramBotUserName(), config.getTelegramGotToken(),
            yandexCastClient, config.getProxyIp(), config.getProxyPort());
        logger.info("Bot started");
        while (true) {
            Thread.sleep(10 * 60 * 1000);
        }
    }

    private static TelegramBot createTelegramBot(String botName, String botToken, Function<String, String> processor, String proxyIp,
                                                 int proxyPort) throws TelegramApiRequestException {
        TelegramBot bot;
        TelegramBotsApi botapi = new TelegramBotsApi();
        DefaultBotOptions botOptions = ApiContext.getInstance(DefaultBotOptions.class);
        botOptions.setProxyHost(proxyIp);
        botOptions.setProxyPort(proxyPort);
        botOptions.setProxyType(DefaultBotOptions.ProxyType.HTTP);
        bot = new TelegramBot(botName, botToken, botOptions, processor);
        bot.addMatcher("https://www.youtube.com.*");
        bot.addMatcher("https://m.youtube.com.*").addReplacement("//m.", "//www.");
        botapi.registerBot(bot);
        return bot;
    }
}
