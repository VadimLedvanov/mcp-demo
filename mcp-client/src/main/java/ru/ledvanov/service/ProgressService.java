package ru.ledvanov.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProgressService {
    private final Map<String, SseEmitter> connections = new ConcurrentHashMap<>();

    public SseEmitter open(String token) {
        SseEmitter emitter = new SseEmitter(120_000L);
        connections.put(token, emitter);

        emitter.onCompletion(() -> connections.remove(token, emitter));
        emitter.onTimeout(() -> {
            connections.remove(token, emitter);
            emitter.complete();
        });

        emitter.onError(_ -> connections.remove(token, emitter));

        return emitter;
    }

    public void send(String token, String event, Object data) {
        SseEmitter emitter = connections.get(token);
        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(event)
                    .data(data));
        } catch (IOException e) {
            connections.remove(token, emitter);
        }
    }

    public void complete(String token) {
        SseEmitter emitter = connections.remove(token);
        if (emitter != null) {
            emitter.complete();
        }
    }
}
