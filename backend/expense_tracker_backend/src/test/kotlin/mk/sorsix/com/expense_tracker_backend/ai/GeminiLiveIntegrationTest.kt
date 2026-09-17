package mk.sorsix.com.expense_tracker_backend.ai

import mk.sorsix.com.expense_tracker_backend.AbstractIntegrationTest
import mk.sorsix.com.expense_tracker_backend.config.AiPrompts
import mk.sorsix.com.expense_tracker_backend.domain.GeminiApiResult
import mk.sorsix.com.expense_tracker_backend.domain.GeminiComparisonResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".*\\S+.*")
class GeminiLiveIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var chatClientBuilder: ChatClient.Builder

    @Autowired
    lateinit var aiPrompts: AiPrompts

    private val savingUserPrompt =
        """
        Spending summary for month: 2026-07-01
        Reported monthly income: 2500.00
        Total spent this month: 162.55

        Spending by category (top-level category, then its subcategories):
        - Food: 162.55 across 2 expense(s)
            - Groceries: 120.55 across 1 expense(s)
            - Dining Out: 42.00 across 1 expense(s)

        Total budget limit to distribute across categories for 2026-08-01: 2000
        """.trimIndent()

    private val comparisonUserPrompt =
        """
        === CURRENT PERIOD ===
        Period: MONTH (2026-07-01 to 2026-07-31)
        Days elapsed: 31 of 31
        Period complete: yes
        Reported monthly income: 2500.00
        Total spent so far: 162.55
        Spending by category (top-level category, then its subcategories):
        - Food: 162.55 across 2 expense(s)
            - Groceries: 120.55 across 1 expense(s)
            - Dining Out: 42.00 across 1 expense(s)

        === EARLIER PERIOD (compare against this) ===
        Period: MONTH (2026-06-01 to 2026-06-30)
        Days elapsed: 30 of 30
        Period complete: yes
        Reported monthly income: 2500.00
        Total spent so far: 300.00
        Spending by category (top-level category, then its subcategories):
        - Food: 300.00 across 3 expense(s)
            - Groceries: 200.00 across 2 expense(s)
            - Dining Out: 100.00 across 1 expense(s)
        """.trimIndent()

    @Test
    fun `saving recommendation prompt returns a usable structured response`() {
        val chatClient = chatClientBuilder.build()

        val result =
            chatClient
                .prompt()
                .system(aiPrompts.monthlySavingRecommendation)
                .user(savingUserPrompt)
                .call()
                .entity(GeminiApiResult::class.java)

        assertThat(result).isNotNull()
        assertThat(result!!.success).isTrue()
        assertThat(result.recommendationMessage).isNotBlank()
        println("Gemini saving recommendation: ${result.recommendationMessage}")
    }

    @Test
    fun `period comparison prompt returns a usable structured response`() {
        val chatClient = chatClientBuilder.build()

        val result =
            chatClient
                .prompt()
                .system(aiPrompts.periodComparison)
                .user(comparisonUserPrompt)
                .call()
                .entity(GeminiComparisonResult::class.java)

        assertThat(result).isNotNull()
        assertThat(result!!.success).isTrue()
        assertThat(result.comparisonMessage).isNotBlank()
        println("Gemini period comparison: ${result.comparisonMessage}")
    }
}
