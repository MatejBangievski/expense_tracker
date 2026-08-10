package mk.sorsix.com.expense_tracker_backend.api

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
class PlanControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `create plan with invalid date range fails`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        mockMvc.post("/api/plans") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"name":"Bad Plan","startDate":"2026-09-20","endDate":"2026-09-10","totalBudget":500.00}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("End date cannot be before start date") }
        }
    }
    @Test
    fun `update nonexistent plan returns not found`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        mockMvc.put("/api/plans/999999") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"name":"X","startDate":"2026-09-10","endDate":"2026-09-20","totalBudget":100.00}"""
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error") { value("Plan not found") }
        }
    }

    @Test
    fun `user cannot update another users plan`() {
        val userA = mockMvc.registerAndLogin(objectMapper)
        val userB = mockMvc.registerAndLogin(objectMapper)

        val planResponse = mockMvc.post("/api/plans") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${userA.accessToken}")
            content = """{"name":"Trip","startDate":"2026-09-10","endDate":"2026-09-20","totalBudget":1000.00}"""
        }.andReturn().response.contentAsString

        val planId = objectMapper.readTree(planResponse).get("id").asLong()

        mockMvc.put("/api/plans/$planId") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${userB.accessToken}")
            content = """{"name":"Hijacked","startDate":"2026-09-10","endDate":"2026-09-20","totalBudget":1.00}"""
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error") { value("You do not own this plan") }
        }
    }

    @Test
    fun `update plan with invalid date range fails`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        val planResponse = mockMvc.post("/api/plans") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"name":"Trip","startDate":"2026-09-10","endDate":"2026-09-20","totalBudget":1000.00}"""
        }.andReturn().response.contentAsString

        val planId = objectMapper.readTree(planResponse).get("id").asLong()

        mockMvc.put("/api/plans/$planId") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"name":"Trip","startDate":"2026-09-20","endDate":"2026-09-10","totalBudget":1000.00}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("End date cannot be before start date") }
        }
    }

    @Test
    fun `delete plan succeeds for owner`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        val planResponse = mockMvc.post("/api/plans") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"name":"Trip","startDate":"2026-09-10","endDate":"2026-09-20","totalBudget":1000.00}"""
        }.andReturn().response.contentAsString

        val planId = objectMapper.readTree(planResponse).get("id").asLong()

        mockMvc.delete("/api/plans/$planId") {
            header("Authorization", "Bearer ${user.accessToken}")
        }.andExpect { status { isNoContent() } }
    }

    @Test
    fun `totalPlanned reflects sum of plan items`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        val planResponse = mockMvc.post("/api/plans") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"name":"Test Trip","startDate":"2026-09-10","endDate":"2026-09-20","totalBudget":2000.00}"""
        }.andReturn().response.contentAsString

        val planId = objectMapper.readTree(planResponse).get("id").asLong()

        mockMvc.post("/api/plans/$planId/items") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":1,"description":"Item 1","plannedDate":"2026-09-13","plannedAmount":150.00}"""
        }

        mockMvc.post("/api/plans/$planId/items") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":null,"description":"Item 2","plannedDate":"2026-09-10","plannedAmount":300.00}"""
        }

        mockMvc.get("/api/plans") {
            header("Authorization", "Bearer ${user.accessToken}")
        }.andExpect {
            status { isOk() }
            jsonPath("$[?(@.id == $planId)].totalPlanned") { value(450.00) }
        }
    }

    @Test
    fun `user cannot delete another users plan`() {
        val userA = mockMvc.registerAndLogin(objectMapper)
        val userB = mockMvc.registerAndLogin(objectMapper)

        val planResponse = mockMvc.post("/api/plans") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${userA.accessToken}")
            content = """{"name":"Private Trip","startDate":"2026-09-10","endDate":"2026-09-20","totalBudget":1000.00}"""
        }.andReturn().response.contentAsString

        val planId = objectMapper.readTree(planResponse).get("id").asLong()

        mockMvc.delete("/api/plans/$planId") {
            header("Authorization", "Bearer ${userB.accessToken}")
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error") { value("You do not own this plan") }
        }
    }
}