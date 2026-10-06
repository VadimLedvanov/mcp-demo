package ru.ledvanov.config;

import io.modelcontextprotocol.client.McpSyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.annotation.McpLogging;
import org.springframework.ai.mcp.annotation.McpProgress;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.ledvanov.service.ProgressService;

import java.util.List;
import java.util.Map;

import static io.modelcontextprotocol.spec.McpSchema.*;

@Configuration
public class AiConfig {
    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);
    private static final String SYSTEM_PROMPT =
            """
            Для создания, получения и просмотра заказов обязательно вызывай инструменты.
            Не придумывай заказы, идентификаторы и результаты операций.
            Сообщай об успешном создании только после успешного ответа инструмента.
            Для создания нескольких заказов вызывай createOrder отдельно для каждого.
            Для списка заказов всегда вызывай getOrders, даже если список есть в истории.
            
            Статусы: NEW — новый, IN_PROGRESS — в работе, DELIVERED — доставлен.
            Не переименовывай статусы и не утверждай, что статус изменён,
            если инструмент этого не подтвердил.
            Если операция недоступна, прямо сообщи об этом.
            
            Каждый запрос на создание заказа — новая операция.
            Даже если аналогичный заказ уже создавался в этом разговоре,
            обязательно заново вызови createOrder.
            Не вычисляй следующий ID самостоятельно.
            Бери ID и статус только из результата текущего вызова инструмента.
            """;

    private final ProgressService progressService;

    public AiConfig(ProgressService progressService) {
        this.progressService = progressService;
    }

    @Bean
    MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    @Bean
    ToolCallbackProvider toolCallbackProvider(List<McpSyncClient> clientList) {
        return SyncMcpToolCallbackProvider.builder()
                .mcpClients(clientList)
                .build();
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder builder,
                          MessageChatMemoryAdvisor messageChatMemoryAdvisor,
                          ToolCallbackProvider mcpTools) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(messageChatMemoryAdvisor)
                .defaultTools(mcpTools)
                .build();
    }

    @McpLogging(clients = "server1")
    public void onServerLog(LoggingMessageNotification notification) {
        log.info("Лог от MCP-сервера [{}]: {}",
                notification.level(),
                notification.data());
    }

    @McpProgress(clients = "server1")
    public void handleProgressNotification(ProgressNotification notification) {
        log.info("Получен MCP-прогресс: token={}, progress={}",
                notification.progressToken(),
                notification.progress());

        Double total = notification.total();
        double percent = total != null && total > 0 ? notification.progress() / total * 100 : 0;

        progressService.send(
                notification.progressToken().toString(),
                "progress",
                Map.of(
                    "percent", percent,
                    "message", notification.message() == null ? "" : notification.message()
                )
        );
    }
}
