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
data class Expense(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: User = User(),
    @ManyToOne
    @JoinColumn(name = "category_id")
    val category: Category = Category(),
    @ManyToOne
    @JoinColumn(name = "plan_id")
    val plan: Plan? = null,
    val description: String? = null,
    val amount: BigDecimal = BigDecimal.ZERO,
    val expenseDate: LocalDate = LocalDate.now(),
    val active: Boolean = true,
)
