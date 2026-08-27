package mk.sorsix.com.expense_tracker_backend.service


import mk.sorsix.com.expense_tracker_backend.config.UserChatClientProvider
import mk.sorsix.com.expense_tracker_backend.domain.*
import mk.sorsix.com.expense_tracker_backend.domain.dto.ChangePasswordResult
import mk.sorsix.com.expense_tracker_backend.domain.dto.UpdateUserRequest
import mk.sorsix.com.expense_tracker_backend.domain.dto.UpdateUserResult
import mk.sorsix.com.expense_tracker_backend.domain.dto.UserResponse
import mk.sorsix.com.expense_tracker_backend.repository.MonthlySavingRepository
import mk.sorsix.com.expense_tracker_backend.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate

@Service
class UserService(
    private val userRepository: UserRepository,
    private val encryptionService: EncryptionService,
    private val userChatClientProvider: UserChatClientProvider,
    private val passwordEncoder: PasswordEncoder,
    private val monthlySavingRepository: MonthlySavingRepository,
    private val clock: Clock,
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

    fun setApiKey(user: User, apiKey: String): String? = getApiKey(userRepository.save(user.copy(aiApiKey = encryptionService.encrypt(apiKey))), masked = true)

    fun getApiKey(user: User, masked: Boolean): String? {
        val plain = user.aiApiKey?.let(encryptionService::decrypt) ?: return null
        return if (masked) "…${plain.takeLast(4)}" else plain
    }

    fun clearApiKey(user: User) = userRepository.save(user.copy(aiApiKey = null))

    fun validateApiKey(apiKey: String): Boolean = userChatClientProvider.validate(apiKey)

    fun changePassword(user: User, currentPassword: String, newPassword: String): ChangePasswordResult {
        if (!passwordEncoder.matches(currentPassword, user.passwordHash)) {
            return ChangePasswordResult.InvalidCurrentPassword
        }
        userRepository.save(user.copy(passwordHash = passwordEncoder.encode(newPassword)!!))
        return ChangePasswordResult.Success
    }

    private fun User.toResponse() = UserResponse(
        id = id,
        displayName = displayName,
        email = email,
        monthlySalary = monthlySalary,
        totalSaved = monthlySavingRepository.sumTotalSavedBefore(id, LocalDate.now(clock).withDayOfMonth(1)),
    )
}