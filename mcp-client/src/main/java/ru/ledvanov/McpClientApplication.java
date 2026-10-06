package ru.ledvanov;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.mcp.annotation.McpLogging;
import org.springframework.ai.mcp.annotation.McpProgress;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import static io.modelcontextprotocol.spec.McpSchema.*;

@SpringBootApplication
public class McpClientApplication {
    private static final Logger log = LoggerFactory.getLogger(McpClientApplication.class);
    private static final String SYSTEM_PROMPT =
        """
        Для создания, получения и просмотра заказов обязательно вызывай инструменты.
        Не придумывай заказы, идентификаторы и результаты операций.
        Сообщай об успешном создании только после успешного ответа инструмента.
        Для создания нескольких заказов вызывай createOrder отдельно для каждого.
        Для списка заказов всегда вызывай getOrders, даже если список есть в истории.
        """;

    static void main(String[] args) {
        SpringApplication.run(McpClientApplication.class, args);
    }

    @McpLogging(clients = "server1")
    public void onServerLog(LoggingMessageNotification notification) {
        log.info("Лог от MCP-сервера [{}]: {}",
                notification.level(),
                notification.data());
    }

    @McpProgress(clients = "server1")
    public void handleProgressNotification(ProgressNotification notification) {
        double percentage = notification.progress() * 100;
        System.out.printf("Progress: {%.2f} - {%s}%n", percentage, notification.message());
    }

    @Bean
    MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    @Bean
    CommandLineRunner checkOrders(
            ChatClient.Builder builder,
            ToolCallbackProvider mcpTools,
            MessageChatMemoryAdvisor messageChatMemoryAdvisor) {

        return args -> {
            var chatClient = builder
                    .defaultTools(mcpTools)
                    .defaultSystem(SYSTEM_PROMPT)
                    .defaultAdvisors(messageChatMemoryAdvisor)
                    .build();


            var conversationId = UUID.randomUUID().toString();

            System.out.println("\nАсистент: Я твой асистент.\n");
            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("\nПользователь: ");
                    System.out.println("\nАсистент: " + chatClient.prompt(scanner.nextLine())
                            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                            .toolContext(Map.of("progressToken", UUID.randomUUID().toString()))
                            .call()
                            .content());
                }
            }
        };
    }
}
