package mk.sorsix.com.expense_tracker_backend

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.UUID

data class TestUser(val email: String, val accessToken: String)

fun MockMvc.registerAndLogin(objectMapper: ObjectMapper): TestUser {
    val email = "test-${UUID.randomUUID()}@test.com"

    val response = this.post("/api/auth/register") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"displayName":"Test User","email":"$email","password":"pass123"}"""
    }.andReturn().response.contentAsString

    val accessToken = objectMapper.readTree(response).get("accessToken").asText()
    return TestUser(email, accessToken)
}