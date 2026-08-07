package mk.sorsix.com.expense_tracker_backend.domain.dto

data class RegisterRequest (
    val displayName: String,
    val email: String,
    val password: String
)
