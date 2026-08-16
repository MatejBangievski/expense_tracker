package mk.sorsix.com.expense_tracker_backend.domain.dto

import java.math.BigDecimal

data class UpdateUserRequest(
    val displayName: String,
    val monthlySalary: BigDecimal
)

data class UserResponse(
    val id: Long,
    val displayName: String,
    val email: String,
    val monthlySalary: BigDecimal,
    val totalSaved: BigDecimal
)

data class SetApiKeyRequest(
    val apiKey: String,
)

data class ApiKeyResponse(
    val apiKey: String?,
)

data class ValidateApiKeyRequest(
    val apiKey: String,
)

data class ValidateApiKeyResponse(
    val valid: Boolean,
)