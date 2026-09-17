package mk.sorsix.com.expense_tracker_backend.domain

data class IssuedTokens(
    val accessToken: String,
    val refreshToken: String,
)
