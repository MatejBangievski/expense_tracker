package mk.sorsix.com.expense_tracker_backend.api

import com.fasterxml.jackson.databind.ObjectMapper
import mk.sorsix.com.expense_tracker_backend.domain.PeriodType
import mk.sorsix.com.expense_tracker_backend.registerAndLogin
import mk.sorsix.com.expense_tracker_backend.repository.UserRepository
import mk.sorsix.com.expense_tracker_backend.service.PeriodSummaryService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.*
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
class ExpenseControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var periodSummaryService: PeriodSummaryService

    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `list expenses requires authentication`() {
        mockMvc.get("/api/expenses").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `create expense with invalid category fails`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        mockMvc.post("/api/expenses") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":999999,"amount":10.00,"expenseDate":"2026-08-06"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("Category not found") }
        }
    }
    @Test
    fun `update nonexistent expense returns not found`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        mockMvc.put("/api/expenses/999999") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":1,"amount":10.00,"expenseDate":"2026-08-06"}"""
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error") { value("Expense not found") }
        }
    }

    @Test
    fun `user cannot delete another users expense`() {
        val userA = mockMvc.registerAndLogin(objectMapper)
        val userB = mockMvc.registerAndLogin(objectMapper)

        val createResponse = mockMvc.post("/api/expenses") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${userA.accessToken}")
            content = """{"categoryId":1,"amount":15.00,"expenseDate":"2026-08-06"}"""
        }.andReturn().response.contentAsString

        val expenseId = objectMapper.readTree(createResponse).get("id").asLong()

        mockMvc.delete("/api/expenses/$expenseId") {
            header("Authorization", "Bearer ${userB.accessToken}")
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error") { value("You do not own this expense") }
        }
    }

    @Test
    fun `filter expenses by date range`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        mockMvc.post("/api/expenses") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":1,"amount":25.00,"expenseDate":"2026-07-15"}"""
        }
        mockMvc.post("/api/expenses") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":1,"amount":35.00,"expenseDate":"2026-08-15"}"""
        }

        mockMvc.get("/api/expenses?periodStart=2026-07-01&periodEnd=2026-07-31") {
            header("Authorization", "Bearer ${user.accessToken}")
        }.andExpect {
            status { isOk() }
            jsonPath("$[?(@.amount == 25.00)]") { exists() }
            jsonPath("$[?(@.amount == 35.00)]") { doesNotExist() }
        }
    }

    @Test
    fun `filter expenses by search term`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        mockMvc.post("/api/expenses") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":1,"amount":40.00,"expenseDate":"2026-08-06","description":"Unique Coffee Order"}"""
        }

        mockMvc.get("/api/expenses?search=coffee") {
            header("Authorization", "Bearer ${user.accessToken}")
        }.andExpect {
            status { isOk() }
            jsonPath("$[?(@.description == 'Unique Coffee Order')]") { exists() }
        }
    }
    @Test
    fun `user cannot create expense against another users private category`() {
        val userA = mockMvc.registerAndLogin(objectMapper)
        val userB = mockMvc.registerAndLogin(objectMapper)

        val createCategoryResponse = mockMvc.post("/api/categories") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${userA.accessToken}")
            content = """{"name":"UserA Private ${System.nanoTime()}"}"""
        }.andReturn().response.contentAsString

        val privateCategoryId = objectMapper.readTree(createCategoryResponse).get("id").asLong()

        mockMvc.post("/api/expenses") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${userB.accessToken}")
            content = """{"categoryId":$privateCategoryId,"amount":10.00,"expenseDate":"2026-08-06"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("Category not found") }
        }
    }

    @Test
    fun `full crud lifecycle for an expense`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        val createResponse = mockMvc.post("/api/expenses") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":1,"amount":45.50,"expenseDate":"2026-08-06","description":"Test expense"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString

        val expenseId = objectMapper.readTree(createResponse).get("id").asLong()

        mockMvc.get("/api/expenses") {
            header("Authorization", "Bearer ${user.accessToken}")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].id") { value(expenseId) }
        }

        mockMvc.put("/api/expenses/$expenseId") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":1,"amount":60.00,"expenseDate":"2026-08-06","description":"Updated"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.amount") { value(60.00) }
        }

        mockMvc.delete("/api/expenses/$expenseId") {
            header("Authorization", "Bearer ${user.accessToken}")
        }.andExpect { status { isNoContent() } }
    }

    @Test
    fun `user cannot update another users expense`() {
        val userA = mockMvc.registerAndLogin(objectMapper)
        val userB = mockMvc.registerAndLogin(objectMapper)

        val createResponse = mockMvc.post("/api/expenses") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${userA.accessToken}")
            content = """{"categoryId":1,"amount":20.00,"expenseDate":"2026-08-06"}"""
        }.andReturn().response.contentAsString

        val expenseId = objectMapper.readTree(createResponse).get("id").asLong()

        mockMvc.put("/api/expenses/$expenseId") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${userB.accessToken}")
            content = """{"categoryId":1,"amount":999.00,"expenseDate":"2026-08-06"}"""
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error") { value("You do not own this expense") }
        }
    }

    @Test
    fun `filter expenses by exact category name`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        mockMvc.post("/api/expenses") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":1,"amount":15.00,"expenseDate":"2026-08-06","description":"Filter test"}"""
        }

        mockMvc.get("/api/expenses?categoryName=Food") {
            header("Authorization", "Bearer ${user.accessToken}")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].categoryName") { value("Food") }
        }
    }

    @Test
    fun `summarizing a completed week locks its expenses`() {
        val user = mockMvc.registerAndLogin(objectMapper)
        val pastWeekDay = LocalDate.of(2025, 1, 7) // Tuesday of the ISO week Mon 2025-01-06..Sun 2025-01-12
        val createResponse = mockMvc.post("/api/expenses") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":5,"amount":50.00,"expenseDate":"$pastWeekDay"}"""
        }.andReturn().response.contentAsString
        val expenseId = objectMapper.readTree(createResponse).get("id").asLong()
        val userId = userRepository.findByEmail(user.email)!!.id

        periodSummaryService.generateForUser(userId, PeriodType.WEEK, pastWeekDay)

        mockMvc.put("/api/expenses/$expenseId") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":5,"amount":99.99,"expenseDate":"$pastWeekDay"}"""
        }.andExpect { status { isConflict() } }

        mockMvc.delete("/api/expenses/$expenseId") {
            header("Authorization", "Bearer ${user.accessToken}")
        }.andExpect { status { isConflict() } }
    }

    @Test
    fun `summarizing the current week leaves its expenses editable`() {
        val user = mockMvc.registerAndLogin(objectMapper)
        val today = LocalDate.now()
        val createResponse = mockMvc.post("/api/expenses") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":5,"amount":50.00,"expenseDate":"$today"}"""
        }.andReturn().response.contentAsString
        val expenseId = objectMapper.readTree(createResponse).get("id").asLong()
        val userId = userRepository.findByEmail(user.email)!!.id

        periodSummaryService.generateForUser(userId, PeriodType.WEEK, today)

        mockMvc.put("/api/expenses/$expenseId") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":5,"amount":75.00,"expenseDate":"$today"}"""
        }.andExpect { status { isOk() } }
    }
}