package mk.sorsix.com.expense_tracker_backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "refresh_token")
data class RefreshToken (
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: User = User(),

    @Column(unique = true)
    val token: String = "",
    val expiresAt: Instant = Instant.now(),
    val revoked: Boolean = false,
    val createdAt: Instant = Instant.now()
)