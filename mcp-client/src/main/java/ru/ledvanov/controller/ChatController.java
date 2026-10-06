package ru.ledvanov.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.ledvanov.dto.AskCreateDto;
import ru.ledvanov.service.ProgressService;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ChatController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final ChatClient chatClient;
    private final ProgressService progressService;

    public ChatController(ChatClient chatClient, ProgressService progressService) {
        this.chatClient = chatClient;
        this.progressService = progressService;
    }

    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter query(@RequestBody AskCreateDto dto,
                            @RequestHeader("X-Conversation-Id") String conversationId) {
        String token = UUID.randomUUID().toString();
        SseEmitter emitter = progressService.open(token);

        Thread.startVirtualThread(() -> {
            try {
                progressService.send(token, "status", Map.of("message", "Обрабатываем запрос"));

                String answer = chatClient.prompt()
                        .user(dto.message())
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .toolContext(Map.of("progressToken", token))
                        .call()
                        .content();

                progressService.send(token, "answer", Map.of("text", Objects.requireNonNull(answer)));
            } catch (Exception exception) {
                log.error("Ошибка обработки запроса {}", token, exception);
                progressService.send(token, "error", Map.of("message", "Не удалось обработать запрос"));
            } finally {
                progressService.complete(token);
            }
        });

        return emitter;
    }
}
