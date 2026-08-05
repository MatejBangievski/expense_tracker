package mk.sorsix.com.expense_tracker_backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "app_user")
data class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val displayName: String = "",
    @Column(unique = true, nullable = false)
    val email: String = "",
    @Column(nullable = false)
    val passwordHash: String = "",
    val monthlySalary: BigDecimal = BigDecimal.ZERO,
    val totalSaved: BigDecimal = BigDecimal.ZERO,
)