import java.nio.file.Path;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class YaCastConfiguration {
    private static final String CONFIG_FILE = "yacast.cfg";
    private long telegramChatId;
    private String telegramBotUserName;
    private String telegramGotToken;
    private String proxyIp;
    private Integer proxyPort;
    private String yaLogin;
    private String yaPasswd;
    private String yaDeviceId;

    public YaCastConfiguration(Path configPath) {
        Config config = ConfigFactory.parseFile(configPath.toFile());
        parseConfig(config);
    }

    public YaCastConfiguration() {
        Config config = ConfigFactory.parseResources(CONFIG_FILE);
        parseConfig(config);
    }

    private void parseConfig(final Config config) {
        if (config.hasPath("telegramChatId")) {
            telegramChatId = config.getLong("telegramChatId");
        }
        if (config.hasPath("telegramBotUserName")) {
            telegramBotUserName = config.getString("telegramBotUserName");
        }
        if (config.hasPath("telegramGotToken")) {
            telegramGotToken = config.getString("telegramGotToken");
        }
        if (config.hasPath("proxyIp")) {
            proxyIp = config.getString("proxyIp");
        }
        if (config.hasPath("proxyPort")) {
            proxyPort = config.getInt("proxyPort");
        }
        if (config.hasPath("yaLogin")) {
            yaLogin = config.getString("yaLogin");
        }
        if (config.hasPath("yaPasswd")) {
            yaPasswd = config.getString("yaPasswd");
        }
        if (config.hasPath("yaDeviceId")) {
            yaDeviceId = config.getString("yaDeviceId");
        }
    }

    public long getTelegramChatId() {
        return telegramChatId;
    }

    public String getTelegramBotUserName() {
        return telegramBotUserName;
    }

    public String getTelegramGotToken() {
        return telegramGotToken;
    }

    public String getProxyIp() {
        return proxyIp;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public String getYaLogin() {
        return yaLogin;
    }

    public String getYaPasswd() {
        return yaPasswd;
    }

    public String getYaDeviceId() {
        return yaDeviceId;
    }
}
