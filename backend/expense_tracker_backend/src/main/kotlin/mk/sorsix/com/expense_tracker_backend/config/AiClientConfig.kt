package mk.sorsix.com.expense_tracker_backend.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AiClientConfig(
    @param:Value("\${app.ai.recommendation.system-prompt}")
    private val recommendationSystemPrompt: String,
) {
    @Bean
    fun chatClient(builder: ChatClient.Builder): ChatClient =
        builder
            .defaultSystem(recommendationSystemPrompt)
            .build()
}