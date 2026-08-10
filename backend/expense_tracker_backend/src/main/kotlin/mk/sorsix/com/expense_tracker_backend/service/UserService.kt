package mk.sorsix.com.expense_tracker_backend.service


import mk.sorsix.com.expense_tracker_backend.domain.*
import mk.sorsix.com.expense_tracker_backend.domain.dto.UpdateUserRequest
import mk.sorsix.com.expense_tracker_backend.domain.dto.UpdateUserResult
import mk.sorsix.com.expense_tracker_backend.domain.dto.UserResponse
import mk.sorsix.com.expense_tracker_backend.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository
) {

    fun getProfile(user: User): UserResponse = user.toResponse()

    fun updateProfile(user: User, request: UpdateUserRequest): UpdateUserResult {
        val existing = userRepository.findById(user.id).orElse(null)
            ?: return UpdateUserResult.UserNotFound

        val updated = userRepository.save(
            existing.copy(displayName = request.displayName, monthlySalary = request.monthlySalary)
        )

        return UpdateUserResult.Success(updated.toResponse())
    }

    private fun User.toResponse() = UserResponse(
        id = id,
        displayName = displayName,
        email = email,
        monthlySalary = monthlySalary,
        totalSaved = totalSaved
    )
}