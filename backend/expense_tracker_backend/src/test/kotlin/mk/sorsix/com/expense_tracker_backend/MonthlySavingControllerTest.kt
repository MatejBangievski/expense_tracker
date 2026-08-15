package mk.sorsix.com.expense_tracker_backend.api

import com.fasterxml.jackson.databind.ObjectMapper
import mk.sorsix.com.expense_tracker_backend.TestUser
import mk.sorsix.com.expense_tracker_backend.registerAndLogin
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.ai.chat.model.ChatModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.LocalDate

/*
    Postoi mozhnost AI modelot da ishalucinira i da padne nekoj test!!
 */
@SpringBootTest
@AutoConfigureMockMvc
class MonthlySavingControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @MockitoBean lateinit var chatModel: ChatModel

    private fun postExpense(user: TestUser, categoryId: Long, amount: String, date: LocalDate) {
        mockMvc.post("/api/expenses") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":$categoryId,"amount":$amount,"expenseDate":"$date"}"""
        }
    }

    private fun generate(user: TestUser) =
        mockMvc.post("/api/monthly-saving-plan/ai") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"budgetLimit":2000}"""
        }

    @Test
    fun `generating a plan requires authentication`() {
        mockMvc.post("/api/monthly-saving-plan/ai") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"budgetLimit":2000}"""
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `manual plan reuses AI persistence with user-provided limits and no message`() {
        val user = mockMvc.registerAndLogin(objectMapper)
        // Both expenses roll up under the top-level "Food" category; actualSpent is auto-filled from the summary.
        postExpense(user, 5, "120.55", LocalDate.now())
        postExpense(user, 6, "42.00", LocalDate.now())

        mockMvc.post("/api/monthly-saving-plan") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """
                {
                  "budgetLimit": 2000,
                  "categoryLimits": [
                    {"categoryName": "Food", "suggestedLimit": 300, "reason": "food cap"}
                  ]
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.savingMonth") { exists() }
            jsonPath("$.recommendationMessage") { value(nullValue()) }
            jsonPath("$.categoryLimits[0].categoryName") { value("Food") }
            jsonPath("$.categoryLimits[0].monthlyLimit") { value(300) }
            jsonPath("$.categoryLimits[0].actualSpent") { value(162.55) }
        }
    }

    @Test
    fun `manual plan requires authentication`() {
        mockMvc.post("/api/monthly-saving-plan") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"budgetLimit":2000,"categoryLimits":[]}"""
        }.andExpect { status { isUnauthorized() } }
    }
}