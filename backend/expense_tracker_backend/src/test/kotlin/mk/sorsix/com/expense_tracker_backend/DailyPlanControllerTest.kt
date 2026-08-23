package mk.sorsix.com.expense_tracker_backend.api

import mk.sorsix.com.expense_tracker_backend.AbstractIntegrationTest

import com.fasterxml.jackson.databind.ObjectMapper
import mk.sorsix.com.expense_tracker_backend.registerAndLogin
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.*

@SpringBootTest
@AutoConfigureMockMvc
class DailyPlanControllerTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private fun createPlan(mockMvc: MockMvc, token: String): Long {
        val response = mockMvc.post("/api/plans") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $token")
            content = """{"name":"Trip","startDate":"2026-09-10","endDate":"2026-09-20","totalBudget":2000.00}"""
        }.andReturn().response.contentAsString
        return objectMapper.readTree(response).get("id").asLong()
    }

    @Test
    fun `daily allocation outside plan range is rejected`() {
        val user = mockMvc.registerAndLogin(objectMapper)
        val planId = createPlan(mockMvc, user.accessToken)

        mockMvc.post("/api/plans/$planId/daily") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"date":"2026-10-01","allocatedAmount":100.00}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("Date is outside the plan's date range") }
        }
    }

    @Test
    fun `create daily plan on nonexistent plan returns not found`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        mockMvc.post("/api/plans/999999/daily") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"date":"2026-09-13","allocatedAmount":100.00}"""
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error") { value("Plan not found") }
        }
    }

    @Test
    fun `user cannot create daily plan on another users plan`() {
        val userA = mockMvc.registerAndLogin(objectMapper)
        val userB = mockMvc.registerAndLogin(objectMapper)
        val planId = createPlan(mockMvc, userA.accessToken)

        mockMvc.post("/api/plans/$planId/daily") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${userB.accessToken}")
            content = """{"date":"2026-09-13","allocatedAmount":100.00}"""
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error") { value("You do not own this plan") }
        }
    }

    @Test
    fun `update nonexistent daily plan returns not found`() {
        val user = mockMvc.registerAndLogin(objectMapper)
        val planId = createPlan(mockMvc, user.accessToken)

        mockMvc.put("/api/plans/$planId/daily/999999") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"date":"2026-09-13","allocatedAmount":100.00}"""
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error") { value("Daily allocation not found") }
        }
    }

    @Test
    fun `user cannot update another users daily plan`() {
        val userA = mockMvc.registerAndLogin(objectMapper)
        val userB = mockMvc.registerAndLogin(objectMapper)
        val planId = createPlan(mockMvc, userA.accessToken)

        val createResponse = mockMvc.post("/api/plans/$planId/daily") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${userA.accessToken}")
            content = """{"date":"2026-09-15","allocatedAmount":100.00}"""
        }.andReturn().response.contentAsString

        val dailyPlanId = objectMapper.readTree(createResponse).get("id").asLong()

        mockMvc.put("/api/plans/$planId/daily/$dailyPlanId") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${userB.accessToken}")
            content = """{"date":"2026-09-15","allocatedAmount":999.00}"""
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error") { value("You do not own this plan") }
        }
    }

    @Test
    fun `duplicate daily allocation for same date is rejected`() {
        val user = mockMvc.registerAndLogin(objectMapper)
        val planId = createPlan(mockMvc, user.accessToken)

        mockMvc.post("/api/plans/$planId/daily") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"date":"2026-09-13","allocatedAmount":200.00}"""
        }.andExpect { status { isOk() } }

        mockMvc.post("/api/plans/$planId/daily") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"date":"2026-09-13","allocatedAmount":50.00}"""
        }.andExpect {
            status { isEqualTo(409) }
            jsonPath("$.error") { value("An allocation already exists for this date") }
        }
    }

    @Test
    fun `full lifecycle for a daily allocation`() {
        val user = mockMvc.registerAndLogin(objectMapper)
        val planId = createPlan(mockMvc, user.accessToken)

        val createResponse = mockMvc.post("/api/plans/$planId/daily") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"date":"2026-09-14","allocatedAmount":180.00}"""
        }.andReturn().response.contentAsString

        val dailyPlanId = objectMapper.readTree(createResponse).get("id").asLong()

        mockMvc.put("/api/plans/$planId/daily/$dailyPlanId") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"date":"2026-09-14","allocatedAmount":220.00}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.allocatedAmount") { value(220.00) }
        }

        mockMvc.delete("/api/plans/$planId/daily/$dailyPlanId") {
            header("Authorization", "Bearer ${user.accessToken}")
        }.andExpect { status { isNoContent() } }
    }
    @Test
    fun `create item on nonexistent plan returns not found`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        mockMvc.post("/api/plans/999999/items") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":1,"description":"Item","plannedDate":"2026-09-13","plannedAmount":100.00}"""
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error") { value("Plan not found") }
        }
    }

    @Test
    fun `update nonexistent item returns not found`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        mockMvc.put("/api/plans/1/items/999999") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":1,"description":"X","plannedDate":"2026-09-13","plannedAmount":50.00}"""
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error") { value("Plan item not found") }
        }
    }
    @Test
    fun `over budget daily allocation requires confirmation and can be confirmed`() {
        val user = mockMvc.registerAndLogin(objectMapper)
        val planId = createPlan(mockMvc, user.accessToken)

        mockMvc.post("/api/plans/$planId/daily") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"date":"2026-09-13","allocatedAmount":2100.00}"""
        }.andExpect {
            status { isEqualTo(409) }
            jsonPath("$.error") { value("over_budget") }
            jsonPath("$.remainingBudget") { value(2000.00) }
        }

        mockMvc.post("/api/plans/$planId/daily") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"date":"2026-09-13","allocatedAmount":2100.00,"confirmOverBudget":true}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.allocatedAmount") { value(2100.00) }
        }
    }

}