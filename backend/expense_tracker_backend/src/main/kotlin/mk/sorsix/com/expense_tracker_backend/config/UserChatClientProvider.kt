package mk.sorsix.com.expense_tracker_backend.config

import com.google.genai.Client
import mk.sorsix.com.expense_tracker_backend.domain.User
import mk.sorsix.com.expense_tracker_backend.service.EncryptionService
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.google.genai.GoogleGenAiChatModel
import org.springframework.stereotype.Component

@Component
class UserChatClientProvider(
    private val defaultChatClient: ChatClient,
    private val baseChatModel: ChatModel,
    private val encryptionService: EncryptionService,
) {
    fun forUser(user: User): ChatClient = user.aiApiKey?.let { clientFor(encryptionService.decrypt(it)) } ?: defaultChatClient

    fun validate(apiKey: String): Boolean =
        try {
            clientFor(apiKey)
                .prompt()
                .user("ping")
                .call()
                .content()
            true
        } catch (ex: Exception) {
            false
        }

    private fun clientFor(apiKey: String): ChatClient {
        val base = baseChatModel as? GoogleGenAiChatModel ?: return defaultChatClient
        val genAiClient = Client.builder().apiKey(apiKey).build()
        val model =
            GoogleGenAiChatModel
                .builder()
                .genAiClient(genAiClient)
                .options(base.options)
                .build()
        return ChatClient.create(model)
    }
}
