package mk.sorsix.com.expense_tracker_backend.domain.dto

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String
)
