import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class YaCastClientTest {
    private static final Logger logger = LogManager.getLogger(YaCastClientTest.class);

    @Test
    void testCast() {
        new YandexCastClient("login", "passwd")
            .withDeviceId("deviceId")
            .playVideo("https://www.youtube.com/watch?v=....");
    }

    @Test
    void showDevices() {
        String login = "";
        String pass = "";
        try (HttpConnection connection = new HttpConnection()) {
            Map<String, String> loginParams = Map.of("login", login, "passwd", pass);
            connection.doPost("https://passport.yandex.ru/passport?mode=auth&retpath=https://yandex.ru", loginParams);
            logger.info(connection.doGet("https://quasar.yandex.ru/devices_online_stats"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
