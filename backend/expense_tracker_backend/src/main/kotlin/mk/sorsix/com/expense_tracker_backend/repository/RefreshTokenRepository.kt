package mk.sorsix.com.expense_tracker_backend.repository

import mk.sorsix.com.expense_tracker_backend.domain.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): RefreshToken?
}