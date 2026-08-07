package mk.sorsix.com.expense_tracker_backend.security

import mk.sorsix.com.expense_tracker_backend.domain.User
import mk.sorsix.com.expense_tracker_backend.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component

@Component
class CurrentUserProvider(
    private val userRepository: UserRepository
) {
    fun resolve(userDetails: UserDetails): User =
        userRepository.findByEmail(userDetails.username)
            ?: throw IllegalStateException("Authenticated user not found in database")
}