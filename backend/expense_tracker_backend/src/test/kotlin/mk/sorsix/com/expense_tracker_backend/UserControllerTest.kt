package mk.sorsix.com.expense_tracker_backend.api

import com.fasterxml.jackson.databind.ObjectMapper
import mk.sorsix.com.expense_tracker_backend.registerAndLogin
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.*

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `get profile requires authentication`() {
        mockMvc.get("/api/users/me").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `update profile changes displayName and salary`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        mockMvc.put("/api/users/me") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"displayName":"Updated Name","monthlySalary":2000.00}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.displayName") { value("Updated Name") }
            jsonPath("$.monthlySalary") { value(2000.00) }
        }
    }

    @Test
    fun `set, read masked and full, then clear the api key`() {
        val user = mockMvc.registerAndLogin(objectMapper)
        val auth = "Bearer ${user.accessToken}"

        mockMvc.put("/api/users/me/api-key") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", auth)
            content = """{"apiKey":"sk-secret-key-1234"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.apiKey") { value("…1234") }
        }

        mockMvc.get("/api/users/me/api-key") {
            header("Authorization", auth)
        }.andExpect {
            status { isOk() }
            jsonPath("$.apiKey") { value("…1234") }
        }

        mockMvc.get("/api/users/me/api-key?masked=false") {
            header("Authorization", auth)
        }.andExpect {
            status { isOk() }
            jsonPath("$.apiKey") { value("sk-secret-key-1234") }
        }

        mockMvc.delete("/api/users/me/api-key") {
            header("Authorization", auth)
        }.andExpect { status { isNoContent() } }

        mockMvc.get("/api/users/me/api-key") {
            header("Authorization", auth)
        }.andExpect {
            status { isOk() }
            jsonPath("$.apiKey") { value(nullValue()) }
        }
    }

    @Test
    fun `api key endpoints require authentication`() {
        mockMvc.get("/api/users/me/api-key").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `validating a bogus key reports it invalid`() {
        val user = mockMvc.registerAndLogin(objectMapper)

        mockMvc.post("/api/users/me/api-key/validate") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer ${user.accessToken}")
            content = """{"apiKey":"definitely-not-a-real-key"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.valid") { value(false) }
        }
    }
}