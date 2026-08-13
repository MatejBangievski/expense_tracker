package mk.sorsix.com.expense_tracker_backend.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate


@Entity
data class PeriodSummary(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User = User(),

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 10)
    val periodType: PeriodType = PeriodType.MONTH,

    @Column(name = "period_start", nullable = false)
    val periodStart: LocalDate = LocalDate.now().withDayOfMonth(1),

    @Column(name = "total_spent", nullable = false, precision = 12, scale = 2)
    val totalSpent: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_income", precision = 12, scale = 2)
    val totalIncome: BigDecimal? = null,
)