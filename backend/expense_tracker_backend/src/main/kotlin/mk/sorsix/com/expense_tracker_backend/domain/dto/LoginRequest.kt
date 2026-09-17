package mk.sorsix.com.expense_tracker_backend.domain.dto

data class LoginRequest(
    val email: String,
    val password: String,
)
