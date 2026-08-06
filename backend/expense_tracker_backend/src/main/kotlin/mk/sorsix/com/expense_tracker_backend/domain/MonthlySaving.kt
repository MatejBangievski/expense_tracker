package mk.sorsix.com.expense_tracker_backend.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
data class MonthlySaving(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: User = User(),

    val savingMonth: LocalDate = LocalDate.now(),
    val totalIncome: BigDecimal? = null,
    val totalBudgetLimit: BigDecimal = BigDecimal.ZERO,
    val totalSpent: BigDecimal = BigDecimal.ZERO,
    val totalSaved: BigDecimal = BigDecimal.ZERO,

    @Column(columnDefinition = "TEXT")
    val recommendationMessage: String? = null,
)