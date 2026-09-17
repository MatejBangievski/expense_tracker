package mk.sorsix.com.expense_tracker_backend.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AiClientConfig {
    @Bean
    fun chatClient(builder: ChatClient.Builder): ChatClient = builder.build()
}
