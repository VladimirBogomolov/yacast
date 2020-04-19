import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class YandexCastClient implements Function<String, String> {
    private final String login;
    private final String password;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Logger logger = LogManager.getLogger(getClass());
    private String deviceId;

    public YandexCastClient(final String login, final String password) {
        this.login = login;
        this.password = password;
    }

    public YandexCastClient withDeviceId(String id) {
        deviceId = id;
        return this;
    }

    @Override
    public String apply(final String s) {
        return playVideo(s);
    }

    public String playVideo(String videoUrl) {
        logger.debug("try play: " + videoUrl);
        try (HttpConnection connection = new HttpConnection()) {
            Map<String, String> loginParams = Map.of("login", login, "passwd", password);
            connection.doPost("https://passport.yandex.ru/passport?mode=auth&retpath=https://yandex.ru", loginParams);
            String token = connection.doGet("https://frontend.vh.yandex.ru/csrf_token");
            String stats = connection.doGet("https://quasar.yandex.ru/devices_online_stats");
            logger.debug("deviceStats: " + stats);
            logger.debug("deviceId: " + deviceId);
            List<String> deviceIds = getDeviceIdsStream(stats)
                .filter(id -> deviceId == null || deviceId.equals(id))
                .collect(Collectors.toList());
            Map<String, String> headers = Map.of("x-csrf-token", token);
            for (String id : deviceIds) {
                ObjectNode params = mapper.createObjectNode();
                params.put("device", id);
                ObjectNode msg = params.putObject("msg");
                msg.put("provider_item_id", videoUrl);
                if (videoUrl.contains("www.youtube")) {
                    msg.put("player_id", "youtube");
                }
                logger.debug("params " + params.toString());
                String answer = connection.doPost("https://yandex.ru/video/station", params, headers);
                logger.debug("answer: " + answer);
                return answer;
            }
            logger.debug("No device for process message");
            return "No device for process message";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Stream<String> getDeviceIdsStream(String stats) {
        try {
            JsonNode node = mapper.readTree(stats);
            return optionalOf(node.path("items"))
                .filter(JsonNode::isArray)
                .stream()
                .map(ArrayNode.class::cast)
                .flatMap(s -> StreamSupport.stream(s.spliterator(), false))
                .filter(item -> optionalOf(item.path("online")).map(JsonNode::asBoolean).orElse(false))
                .filter(item -> optionalOf(item.path("screen_present")).map(JsonNode::asBoolean).orElse(false))
                .map(item -> optionalOf(item.path("id")))
                .filter(Optional::isPresent)
                .map(o -> o.get().asText());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Optional<JsonNode> optionalOf(JsonNode o) {
        return Optional.of(o)
                       .filter(j -> !j.isMissingNode())
                       .filter(j -> !emptyValue(j.asText()));
    }

    private static boolean emptyValue(String s) {
        Pattern regex = Pattern.compile("^[\\s\t\n]+$", Pattern.DOTALL);
        Matcher regexMatcher = regex.matcher(s);
        return regexMatcher.find();
    }
}
