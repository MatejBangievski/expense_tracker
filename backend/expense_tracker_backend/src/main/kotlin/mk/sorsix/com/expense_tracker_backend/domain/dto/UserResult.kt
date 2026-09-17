package mk.sorsix.com.expense_tracker_backend.domain.dto

sealed class UpdateUserResult {
    data class Success(
        val user: UserResponse,
    ) : UpdateUserResult()

    object UserNotFound : UpdateUserResult()
}

sealed class ChangePasswordResult {
    object Success : ChangePasswordResult()

    object InvalidCurrentPassword : ChangePasswordResult()
}
