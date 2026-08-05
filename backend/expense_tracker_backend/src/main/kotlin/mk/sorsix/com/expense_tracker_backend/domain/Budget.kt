package mk.sorsix.com.expense_tracker_backend.domain

import jakarta.persistence.*
import java.math.BigDecimal

import java.time.LocalDate

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

    val budgetMonth: LocalDate = LocalDate.now(),
    val monthlyLimit: BigDecimal = BigDecimal.ZERO,

)