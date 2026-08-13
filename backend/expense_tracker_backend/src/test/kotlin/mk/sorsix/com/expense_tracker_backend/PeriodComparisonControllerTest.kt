package mk.sorsix.com.expense_tracker_backend.api

import com.fasterxml.jackson.databind.ObjectMapper
import mk.sorsix.com.expense_tracker_backend.TestUser
import mk.sorsix.com.expense_tracker_backend.registerAndLogin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.LocalDate

/*
    Postoi mozhnost AI modelot da ishalucinira i da padne nekoj test!!
 */
@SpringBootTest
@AutoConfigureMockMvc
class PeriodComparisonControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private fun postExpense(user: TestUser, categoryId: Long, amount: String, date: LocalDate) {
        mockMvc.post("/api/expenses") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":$categoryId,"amount":$amount,"expenseDate":"$date"}"""
        }
    }

    private fun compareWithPrevious(user: TestUser) =
        mockMvc.post("/api/period-comparisons/previous?periodType=MONTH") {
            header("Authorization", "Bearer ${user.accessToken}")
        }

    @Test
    fun `comparison requires authentication`() {
        mockMvc.post("/api/period-comparisons/previous?periodType=MONTH")
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".*\\S+.*")
    fun `comparing two months with spending returns a message`() {
        val user = mockMvc.registerAndLogin(objectMapper)
        postExpense(user, 5, "80.00", LocalDate.now())
        postExpense(user, 6, "20.00", LocalDate.now())
        postExpense(user, 5, "150.00", LocalDate.now().minusMonths(1))
        postExpense(user, 6, "60.00", LocalDate.now().minusMonths(1))

        compareWithPrevious(user).andExpect {
            status { isOk() }
            jsonPath("$.id") { exists() }
            jsonPath("$.periodType") { value("MONTH") }
            jsonPath("$.currentPeriodStart") { exists() }
            jsonPath("$.previousPeriodStart") { exists() }
            jsonPath("$.comparisonMessage") { isNotEmpty() }
        }
    }

    @Test
    fun `comparing periods of different types returns 400`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        mockMvc.post("/api/period-comparisons") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"currentPeriodType":"WEEK","currentDate":"2026-07-06","previousPeriodType":"MONTH","previousDate":"2026-06-01"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { exists() }
        }
    }
}