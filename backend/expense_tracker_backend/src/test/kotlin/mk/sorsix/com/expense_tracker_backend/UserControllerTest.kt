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
}