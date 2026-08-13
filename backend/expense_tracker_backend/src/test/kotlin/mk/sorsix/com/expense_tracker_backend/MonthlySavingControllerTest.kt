package mk.sorsix.com.expense_tracker_backend.api

import com.fasterxml.jackson.databind.ObjectMapper
import mk.sorsix.com.expense_tracker_backend.TestUser
import mk.sorsix.com.expense_tracker_backend.registerAndLogin
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
        mockMvc.post("/api/monthly-saving-plan") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"budgetLimit":2000}"""
        }

    @Test
    fun `generating a plan requires authentication`() {
        mockMvc.post("/api/monthly-saving-plan") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"budgetLimit":2000}"""
        }.andExpect { status { isUnauthorized() } }
    }
}