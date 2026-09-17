package mk.sorsix.com.expense_tracker_backend.security

sealed class TokenValidationResult {
    data class Valid(
        val email: String,
    ) : TokenValidationResult()

    object Invalid : TokenValidationResult()
}
