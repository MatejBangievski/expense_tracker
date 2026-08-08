package mk.sorsix.com.expense_tracker_backend.domain

import jakarta.persistence.*
import java.math.BigDecimal


@Entity
data class  MonthlySummaryCategory(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monthly_summary_id", nullable = false)
    val monthlySummary: MonthlySummary = MonthlySummary(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    val category: Category = Category(),

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    val totalAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "expense_count", nullable = false)
    val expenseCount: Int = 0,
)