package mk.sorsix.com.expense_tracker_backend.api

import com.fasterxml.jackson.databind.ObjectMapper
import mk.sorsix.com.expense_tracker_backend.AbstractIntegrationTest
import mk.sorsix.com.expense_tracker_backend.registerAndLogin
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.*

@SpringBootTest
@AutoConfigureMockMvc
class BudgetControllerTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `create budget then reject duplicate for same category and month`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        mockMvc
            .post("/api/budgets") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${user.accessToken}")
                content = """{"categoryId":1,"budgetMonth":"2026-08-01","monthlyLimit":400.00}"""
            }.andExpect { status { isOk() } }

        mockMvc
            .post("/api/budgets") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${user.accessToken}")
                content = """{"categoryId":1,"budgetMonth":"2026-08-01","monthlyLimit":500.00}"""
            }.andExpect {
                status { isEqualTo(409) }
                jsonPath("$.error") { value("A budget already exists for this category and month") }
            }
    }

    @Test
    fun `create budget with invalid category fails`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        mockMvc
            .post("/api/budgets") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${user.accessToken}")
                content = """{"categoryId":999999,"budgetMonth":"2026-08-01","monthlyLimit":100.00}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { value("Category not found") }
            }
    }

    @Test
    fun `update nonexistent budget returns not found`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        mockMvc
            .put("/api/budgets/999999") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${user.accessToken}")
                content = """{"monthlyLimit":100.00}"""
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.error") { value("Budget not found") }
            }
    }

    @Test
    fun `user cannot update another users budget`() {
        val userA = mockMvc.registerAndLogin(objectMapper)
        val userB = mockMvc.registerAndLogin(objectMapper)

        val createResponse =
            mockMvc
                .post("/api/budgets") {
                    contentType = MediaType.APPLICATION_JSON
                    header("Authorization", "Bearer ${userA.accessToken}")
                    content = """{"categoryId":3,"budgetMonth":"2026-08-01","monthlyLimit":75.00}"""
                }.andReturn()
                .response.contentAsString

        val budgetId = objectMapper.readTree(createResponse).get("id").asLong()

        mockMvc
            .put("/api/budgets/$budgetId") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${userB.accessToken}")
                content = """{"monthlyLimit":999.00}"""
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.error") { value("You do not own this budget") }
            }
    }

    @Test
    fun `delete budget succeeds for owner`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        val createResponse =
            mockMvc
                .post("/api/budgets") {
                    contentType = MediaType.APPLICATION_JSON
                    header("Authorization", "Bearer ${user.accessToken}")
                    content = """{"categoryId":4,"budgetMonth":"2026-08-01","monthlyLimit":50.00}"""
                }.andReturn()
                .response.contentAsString

        val budgetId = objectMapper.readTree(createResponse).get("id").asLong()

        mockMvc
            .delete("/api/budgets/$budgetId") {
                header("Authorization", "Bearer ${user.accessToken}")
            }.andExpect { status { isNoContent() } }
    }

    @Test
    fun `actualSpent reflects logged expenses`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        mockMvc.post("/api/budgets") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":1,"budgetMonth":"2026-08-01","monthlyLimit":400.00}"""
        }

        mockMvc.post("/api/expenses") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":1,"amount":120.00,"expenseDate":"2026-08-10"}"""
        }

        mockMvc
            .get("/api/budgets?month=2026-08-01") {
                header("Authorization", "Bearer ${user.accessToken}")
            }.andExpect {
                status { isOk() }
                jsonPath("$[0].actualSpent") { value(120.00) }
            }
    }

    @Test
    fun `update budget limit`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        val createResponse =
            mockMvc
                .post("/api/budgets") {
                    contentType = MediaType.APPLICATION_JSON
                    header("Authorization", "Bearer ${user.accessToken}")
                    content = """{"categoryId":2,"budgetMonth":"2026-08-01","monthlyLimit":150.00}"""
                }.andReturn()
                .response.contentAsString

        val budgetId = objectMapper.readTree(createResponse).get("id").asLong()

        mockMvc
            .put("/api/budgets/$budgetId") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${user.accessToken}")
                content = """{"monthlyLimit":200.00}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.monthlyLimit") { value(200.00) }
            }
    }

    @Test
    fun `user cannot create budget against another users private category`() {
        val userA = mockMvc.registerAndLogin(objectMapper)
        val userB = mockMvc.registerAndLogin(objectMapper)

        val categoryResponse =
            mockMvc
                .post("/api/categories") {
                    contentType = MediaType.APPLICATION_JSON
                    header("Authorization", "Bearer ${userA.accessToken}")
                    content = """{"name":"UserA Private ${System.nanoTime()}"}"""
                }.andReturn()
                .response.contentAsString

        val privateCategoryId = objectMapper.readTree(categoryResponse).get("id").asLong()

        mockMvc
            .post("/api/budgets") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${userB.accessToken}")
                content = """{"categoryId":$privateCategoryId,"budgetMonth":"2026-08-01","monthlyLimit":100.00}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { value("Category not found") }
            }
    }

    @Test
    fun `user cannot delete another users budget`() {
        val userA = mockMvc.registerAndLogin(objectMapper)
        val userB = mockMvc.registerAndLogin(objectMapper)

        val createResponse =
            mockMvc
                .post("/api/budgets") {
                    contentType = MediaType.APPLICATION_JSON
                    header("Authorization", "Bearer ${userA.accessToken}")
                    content = """{"categoryId":4,"budgetMonth":"2026-08-01","monthlyLimit":60.00}"""
                }.andReturn()
                .response.contentAsString

        val budgetId = objectMapper.readTree(createResponse).get("id").asLong()

        mockMvc
            .delete("/api/budgets/$budgetId") {
                header("Authorization", "Bearer ${userB.accessToken}")
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.error") { value("You do not own this budget") }
            }
    }
}
