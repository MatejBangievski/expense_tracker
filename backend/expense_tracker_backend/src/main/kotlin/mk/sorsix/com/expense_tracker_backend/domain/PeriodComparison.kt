package mk.sorsix.com.expense_tracker_backend.domain

import jakarta.persistence.*


@Entity
data class PeriodComparison(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User = User(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_summary_id", nullable = false)
    val currentSummary: PeriodSummary = PeriodSummary(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_summary_id", nullable = false)
    val previousSummary: PeriodSummary = PeriodSummary(),

    @Column(name = "comparison_message", columnDefinition = "TEXT")
    val comparisonMessage: String? = null,
)