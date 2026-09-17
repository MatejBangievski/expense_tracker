package mk.sorsix.com.expense_tracker_backend

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.*

@SpringBootTest
@AutoConfigureMockMvc
class CategoryControllerTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `list categories requires authentication`() {
        mockMvc.get("/api/categories").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `create custom category succeeds`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        mockMvc
            .post("/api/categories") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${user.accessToken}")
                content = """{"name":"Test Category ${System.nanoTime()}"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.default") { value(false) }
            }
    }

    @Test
    fun `cannot edit a default category`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        // Assumes category id 1 is a seeded default (e.g. "Food")
        mockMvc
            .put("/api/categories/1") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${user.accessToken}")
                content = """{"name":"Hacked Name"}"""
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.error") { value("Default categories cannot be edited") }
            }
    }

    @Test
    fun `create category with nonexistent parent fails`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        mockMvc
            .post("/api/categories") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${user.accessToken}")
                content = """{"name":"Test ${System.nanoTime()}","parentCategoryId":999999}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { value("Parent category not found") }
            }
    }

    @Test
    fun `create category with duplicate name fails`() {
        val user = mockMvc.registerAndLogin(objectMapper)
        val name = "Dup ${System.nanoTime()}"

        mockMvc
            .post("/api/categories") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${user.accessToken}")
                content = """{"name":"$name"}"""
            }.andExpect { status { isOk() } }

        mockMvc
            .post("/api/categories") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${user.accessToken}")
                content = """{"name":"$name"}"""
            }.andExpect {
                status { isEqualTo(409) }
                jsonPath("$.error") { value("Category name already exists") }
            }
    }

    @Test
    fun `update nonexistent category returns not found`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        mockMvc
            .put("/api/categories/999999") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${user.accessToken}")
                content = """{"name":"Doesn't matter"}"""
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.error") { value("Category not found") }
            }
    }

    @Test
    fun `user cannot edit another users custom category`() {
        val userA = mockMvc.registerAndLogin(objectMapper)
        val userB = mockMvc.registerAndLogin(objectMapper)

        val createResponse =
            mockMvc
                .post("/api/categories") {
                    contentType = MediaType.APPLICATION_JSON
                    header("Authorization", "Bearer ${userA.accessToken}")
                    content = """{"name":"Private ${System.nanoTime()}"}"""
                }.andReturn()
                .response.contentAsString

        val categoryId = objectMapper.readTree(createResponse).get("id").asLong()

        mockMvc
            .put("/api/categories/$categoryId") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${userB.accessToken}")
                content = """{"name":"Hijacked"}"""
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.error") { value("You do not own this category") }
            }
    }

    @Test
    fun `cannot delete default category`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        mockMvc
            .delete("/api/categories/1") {
                header("Authorization", "Bearer ${user.accessToken}")
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.error") { value("Default categories cannot be deleted") }
            }
    }

    @Test
    fun `cannot delete category that is in use`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        val createResponse =
            mockMvc
                .post("/api/categories") {
                    contentType = MediaType.APPLICATION_JSON
                    header("Authorization", "Bearer ${user.accessToken}")
                    content = """{"name":"InUse ${System.nanoTime()}"}"""
                }.andReturn()
                .response.contentAsString

        val categoryId = objectMapper.readTree(createResponse).get("id").asLong()

        mockMvc
            .post("/api/expenses") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${user.accessToken}")
                content = """{"categoryId":$categoryId,"amount":10.00,"expenseDate":"2026-08-06"}"""
            }.andExpect { status { isOk() } }

        mockMvc
            .delete("/api/categories/$categoryId") {
                header("Authorization", "Bearer ${user.accessToken}")
            }.andExpect {
                status { isEqualTo(409) }
                jsonPath("$.error") { value("Category is in use and cannot be deleted") }
            }
    }

    @Test
    fun `owner can update and delete their own custom category`() {
        val user = mockMvc.registerAndLogin(objectMapper)
        val uniqueName = "Test Category ${System.nanoTime()}"

        val createResponse =
            mockMvc
                .post("/api/categories") {
                    contentType = MediaType.APPLICATION_JSON
                    header("Authorization", "Bearer ${user.accessToken}")
                    content = """{"name":"$uniqueName"}"""
                }.andReturn()
                .response.contentAsString

        val categoryId = objectMapper.readTree(createResponse).get("id").asLong()

        mockMvc
            .put("/api/categories/$categoryId") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${user.accessToken}")
                content = """{"name":"$uniqueName Updated"}"""
            }.andExpect { status { isOk() } }

        mockMvc
            .delete("/api/categories/$categoryId") {
                header("Authorization", "Bearer ${user.accessToken}")
            }.andExpect { status { isNoContent() } }
    }
}
