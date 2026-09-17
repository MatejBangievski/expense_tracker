package mk.sorsix.com.expense_tracker_backend.domain

sealed class RegisterResult {
    data class Success(
        val accessToken: String,
        val refreshToken: String,
    ) : RegisterResult()

    object EmailAlreadyInUse : RegisterResult()
}

sealed class LoginResult {
    data class Success(
        val accessToken: String,
        val refreshToken: String,
    ) : LoginResult()

    object InvalidCredentials : LoginResult()
}

sealed class RefreshResult {
    data class Success(
        val accessToken: String,
        val refreshToken: String,
    ) : RefreshResult()

    object InvalidToken : RefreshResult()

    object ExpiredOrRevoked : RefreshResult()
}

sealed class LogoutResult {
    object Success : LogoutResult()

    object TokenNotFound : LogoutResult()
}
