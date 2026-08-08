package mk.sorsix.com.expense_tracker_backend.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate


@Entity
data class MonthlySummary(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User = User(),

    @Column(name = "summary_month", nullable = false)
    val summaryMonth: LocalDate = LocalDate.now().withDayOfMonth(1),

    @Column(name = "total_spent", nullable = false, precision = 12, scale = 2)
    val totalSpent: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_income", precision = 12, scale = 2)
    val totalIncome: BigDecimal? = null,
)