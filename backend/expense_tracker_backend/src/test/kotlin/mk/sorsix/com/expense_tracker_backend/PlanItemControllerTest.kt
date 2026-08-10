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
class PlanItemControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private fun createPlan(token: String): Long {
        val response = mockMvc.post("/api/plans") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $token")
            content = """{"name":"Trip","startDate":"2026-09-10","endDate":"2026-09-20","totalBudget":2000.00}"""
        }.andReturn().response.contentAsString
        return objectMapper.readTree(response).get("id").asLong()
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
    fun `user cannot create item on another users plan`() {
        val userA = mockMvc.registerAndLogin(objectMapper)
        val userB = mockMvc.registerAndLogin(objectMapper)
        val planId = createPlan(userA.accessToken)

        mockMvc.post("/api/plans/$planId/items") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${userB.accessToken}")
            content = """{"categoryId":1,"description":"Item","plannedDate":"2026-09-13","plannedAmount":100.00}"""
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error") { value("You do not own this plan") }
        }
    }

    @Test
    fun `create item with invalid category fails`() {
        val user = mockMvc.registerAndLogin(objectMapper)
        val planId = createPlan(user.accessToken)

        mockMvc.post("/api/plans/$planId/items") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":999999,"description":"Item","plannedDate":"2026-09-13","plannedAmount":100.00}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("Category not found") }
        }
    }

    @Test
    fun `create item with no category succeeds`() {
        val user = mockMvc.registerAndLogin(objectMapper)
        val planId = createPlan(user.accessToken)

        mockMvc.post("/api/plans/$planId/items") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":null,"description":"Hotel deposit","plannedDate":"2026-09-10","plannedAmount":300.00}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.categoryId") { doesNotExist() }
        }
    }

    @Test
    fun `full crud lifecycle for a plan item`() {
        val user = mockMvc.registerAndLogin(objectMapper)
        val planId = createPlan(user.accessToken)

        val createResponse = mockMvc.post("/api/plans/$planId/items") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":1,"description":"Boat cruise","plannedDate":"2026-09-13","plannedAmount":150.00}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString

        val itemId = objectMapper.readTree(createResponse).get("id").asLong()

        mockMvc.get("/api/plans/$planId/items") {
            header("Authorization", "Bearer ${user.accessToken}")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].id") { value(itemId) }
        }

        mockMvc.put("/api/plans/$planId/items/$itemId") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"categoryId":1,"description":"Boat cruise upgraded","plannedDate":"2026-09-13","plannedAmount":200.00}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.plannedAmount") { value(200.00) }
        }

        mockMvc.delete("/api/plans/$planId/items/$itemId") {
            header("Authorization", "Bearer ${user.accessToken}")
        }.andExpect { status { isNoContent() } }
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
    fun `user cannot update another users plan item`() {
        val userA = mockMvc.registerAndLogin(objectMapper)
        val userB = mockMvc.registerAndLogin(objectMapper)
        val planId = createPlan(userA.accessToken)

        val createResponse = mockMvc.post("/api/plans/$planId/items") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${userA.accessToken}")
            content = """{"categoryId":1,"description":"Private item","plannedDate":"2026-09-13","plannedAmount":100.00}"""
        }.andReturn().response.contentAsString

        val itemId = objectMapper.readTree(createResponse).get("id").asLong()

        mockMvc.put("/api/plans/$planId/items/$itemId") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${userB.accessToken}")
            content = """{"categoryId":1,"description":"Hijacked","plannedDate":"2026-09-13","plannedAmount":999.00}"""
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error") { value("You do not own this plan") }
        }
    }

    @Test
    fun `user cannot use another users private category on a plan item`() {
        val userA = mockMvc.registerAndLogin(objectMapper)
        val userB = mockMvc.registerAndLogin(objectMapper)
        val planId = createPlan(userB.accessToken)

        val categoryResponse = mockMvc.post("/api/categories") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${userA.accessToken}")
            content = """{"name":"UserA Private ${System.nanoTime()}"}"""
        }.andReturn().response.contentAsString

        val privateCategoryId = objectMapper.readTree(categoryResponse).get("id").asLong()

        mockMvc.post("/api/plans/$planId/items") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${userB.accessToken}")
            content = """{"categoryId":$privateCategoryId,"description":"X","plannedDate":"2026-09-13","plannedAmount":50.00}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("Category not found") }
        }
    }
}