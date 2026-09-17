package mk.sorsix.com.expense_tracker_backend.api

import com.fasterxml.jackson.databind.ObjectMapper
import mk.sorsix.com.expense_tracker_backend.AbstractIntegrationTest
import mk.sorsix.com.expense_tracker_backend.TestUser
import mk.sorsix.com.expense_tracker_backend.registerAndLogin
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.model.ChatModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.LocalDate

/*
    Postoi mozhnost AI modelot da ishalucinira i da padne nekoj test!!
 */
@SpringBootTest
@AutoConfigureMockMvc
class MonthlySavingControllerTest : AbstractIntegrationTest() {
    @Autowired lateinit var mockMvc: MockMvc

    @Autowired lateinit var objectMapper: ObjectMapper

    @MockitoBean lateinit var chatModel: ChatModel

    private fun postExpense(
        user: TestUser,
        categoryId: Long,
        amount: String,
        date: LocalDate,
    ) {
        mockMvc.post("/api/expenses") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":$categoryId,"amount":$amount,"expenseDate":"$date"}"""
        }
    }

    @Test
    fun `generating a plan requires authentication`() {
        mockMvc
            .post("/api/monthly-saving-plan/ai") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"budgetLimit":2000}"""
            }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `manual plan reuses AI persistence with user-provided limits and no message`() {
        val user = mockMvc.registerAndLogin(objectMapper)
        postExpense(user, 5, "120.55", LocalDate.now())
        postExpense(user, 6, "42.00", LocalDate.now())

        mockMvc
            .post("/api/monthly-saving-plan") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${user.accessToken}")
                content =
                    """
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
    fun `manual plan floors a category limit to spend and projects saved as budget minus spent`() {
        val user = mockMvc.registerAndLogin(objectMapper)
        postExpense(user, 5, "500.50", LocalDate.now())

        mockMvc
            .post("/api/monthly-saving-plan") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${user.accessToken}")
                content = """{"budgetLimit":2000,"categoryLimits":[{"categoryName":"Food","suggestedLimit":100}]}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.totalBudgetLimit") { value(2000) }
                jsonPath("$.totalSpent") { value(500.5) }
                jsonPath("$.totalSaved") { value(1499.5) }
                jsonPath("$.categoryLimits[0].categoryName") { value("Food") }
                jsonPath("$.categoryLimits[0].monthlyLimit") { value(500.5) }
            }
    }

    @Test
    fun `adding an expense over its category limit warns, then saves once confirmed`() {
        val user = mockMvc.registerAndLogin(objectMapper)
        createPlan(user, budget = 1000, food = 200)

        mockMvc
            .post("/api/expenses") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${user.accessToken}")
                content = """{"categoryId":5,"amount":250,"expenseDate":"${LocalDate.now()}"}"""
            }.andExpect {
                status { isConflict() }
                jsonPath("$.error") { value("over_budget") }
                jsonPath("$.message") { value(containsString("Food")) }
            }

        mockMvc
            .post("/api/expenses") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${user.accessToken}")
                content = """{"categoryId":5,"amount":250,"expenseDate":"${LocalDate.now()}","confirmOverBudget":true}"""
            }.andExpect { status { isOk() } }
    }

    @Test
    fun `adding an expense over the total budget warns`() {
        val user = mockMvc.registerAndLogin(objectMapper)
        createPlan(user, budget = 300, food = 1000)

        mockMvc
            .post("/api/expenses") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${user.accessToken}")
                content = """{"categoryId":5,"amount":350,"expenseDate":"${LocalDate.now()}"}"""
            }.andExpect {
                status { isConflict() }
                jsonPath("$.error") { value("over_budget") }
                jsonPath("$.message") { value(containsString("monthly budget")) }
            }
    }

    @Test
    fun `current plan reflects expenses added after it was created`() {
        val user = mockMvc.registerAndLogin(objectMapper)
        createPlan(user, budget = 1000, food = 200)
        postExpense(user, 5, "50.00", LocalDate.now())

        mockMvc
            .get("/api/monthly-saving-plan") {
                header("Authorization", "Bearer ${user.accessToken}")
            }.andExpect {
                status { isOk() }
                jsonPath("$.totalSpent") { value(50.0) }
                jsonPath("$.totalSaved") { value(950.0) }
                jsonPath("$.categoryLimits[0].actualSpent") { value(50.0) }
            }
    }

    private fun createPlan(
        user: TestUser,
        budget: Int,
        food: Int,
    ) {
        mockMvc
            .post("/api/monthly-saving-plan") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${user.accessToken}")
                content = """{"budgetLimit":$budget,"categoryLimits":[{"categoryName":"Food","suggestedLimit":$food}]}"""
            }.andExpect { status { isOk() } }
    }

    @Test
    fun `manual plan requires authentication`() {
        mockMvc
            .post("/api/monthly-saving-plan") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"budgetLimit":2000,"categoryLimits":[]}"""
            }.andExpect { status { isUnauthorized() } }
    }
}
