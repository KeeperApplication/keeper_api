package xyz.piod.keeper.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BroadcastService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String KEEPER_EVENTS_CHANNEL = "keeper-events";

    public void broadcast(String topic, String event, Object payload) {
        try {
            Map<String, Object> messageMap = Map.of(
                    "topic", topic,
                    "event", event,
                    "payload", payload
            );

            String jsonMessage = objectMapper.writeValueAsString(messageMap);

            redisTemplate.convertAndSend(KEEPER_EVENTS_CHANNEL, jsonMessage);
            log.info("Broadcasted event '{}' to Redis channel '{}'", event, KEEPER_EVENTS_CHANNEL);

        } catch (JsonProcessingException e) {
            log.error("Error serializing broadcast message to JSON: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error publishing message to Redis channel {}: {}", KEEPER_EVENTS_CHANNEL, e.getMessage());
        }
    }
}