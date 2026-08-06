package mk.sorsix.com.expense_tracker_backend.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "daily_plan")
data class DailyPlan(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "plan_id")
    val plan: Plan = Plan(),

    val date: LocalDate = LocalDate.now(),
    val allocatedAmount: BigDecimal = BigDecimal.ZERO
)