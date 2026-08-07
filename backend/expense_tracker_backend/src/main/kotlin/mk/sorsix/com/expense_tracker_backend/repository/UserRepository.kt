package mk.sorsix.com.expense_tracker_backend.repository

import mk.sorsix.com.expense_tracker_backend.domain.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long>{
    fun findByEmail(email: String): User?
}