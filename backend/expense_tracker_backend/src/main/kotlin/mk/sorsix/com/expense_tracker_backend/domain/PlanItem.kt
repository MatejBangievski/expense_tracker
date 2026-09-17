package mk.sorsix.com.expense_tracker_backend.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.math.BigDecimal
import java.time.LocalDate

@Entity
data class PlanItem(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne
    @JoinColumn(name = "plan_id")
    val plan: Plan = Plan(),
    @ManyToOne
    @JoinColumn(name = "category_id")
    val category: Category? = null,
    val description: String = "",
    val plannedDate: LocalDate = LocalDate.now(),
    val plannedAmount: BigDecimal = BigDecimal.ZERO,
)
