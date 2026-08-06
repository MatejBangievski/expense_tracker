package mk.sorsix.com.expense_tracker_backend.domain

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
data class Budget(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: User = User(),

    @ManyToOne
    @JoinColumn(name = "category_id")
    val category: Category = Category(),

    @ManyToOne
    @JoinColumn(name = "monthly_saving_id")
    val monthlySaving: MonthlySaving = MonthlySaving(),

    val monthlyLimit: BigDecimal = BigDecimal.ZERO,
    val actualSpent: BigDecimal? = null,
    val reason: String? = null,

)