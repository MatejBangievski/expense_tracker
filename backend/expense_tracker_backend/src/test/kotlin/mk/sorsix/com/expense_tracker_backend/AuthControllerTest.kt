package mk.sorsix.com.expense_tracker_backend

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `register with new email succeeds`() {
        val email = "test-${UUID.randomUUID()}@test.com"
        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"displayName":"Test User","email":"$email","password":"pass123"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.accessToken") { exists() }
                jsonPath("$.refreshToken") { exists() }
            }
    }

    @Test
    fun `register with duplicate email fails`() {
        val email = "test-${UUID.randomUUID()}@test.com"
        val body = """{"displayName":"Test User","email":"$email","password":"pass123"}"""

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

        mockMvc
            .post("/api/auth/register") {
                contentType = MediaType.APPLICATION_JSON
                content = body
            }.andExpect {
                status { isEqualTo(409) }
                jsonPath("$.error") { value("Email already in use") }
            }
    }

    @Test
    fun `login with wrong password fails`() {
        val email = "test-${UUID.randomUUID()}@test.com"
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"displayName":"Test User","email":"$email","password":"correctpass"}"""
        }

        mockMvc
            .post("/api/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"email":"$email","password":"wrongpass"}"""
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.error") { value("Invalid credentials") }
            }
    }

    @Test
    fun `refresh token rotation invalidates old token`() {
        val email = "test-${UUID.randomUUID()}@test.com"
        val registerResponse =
            mockMvc
                .post("/api/auth/register") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"displayName":"Test User","email":"$email","password":"pass123"}"""
                }.andReturn()
                .response.contentAsString

        val refreshToken =
            com.fasterxml.jackson.databind
                .ObjectMapper()
                .readTree(registerResponse)
                .get("refreshToken")
                .asText()

        // First refresh should succeed
        mockMvc
            .post("/api/auth/refresh") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"refreshToken":"$refreshToken"}"""
            }.andExpect { status { isOk() } }

        // Reusing the same (now-revoked) token should fail
        mockMvc
            .post("/api/auth/refresh") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"refreshToken":"$refreshToken"}"""
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.error") { value("Refresh token expired or revoked") }
            }
    }
}
