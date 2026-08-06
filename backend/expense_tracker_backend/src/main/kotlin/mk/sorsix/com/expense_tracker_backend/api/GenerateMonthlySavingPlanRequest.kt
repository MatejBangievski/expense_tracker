package mk.sorsix.com.expense_tracker_backend.api

import java.math.BigDecimal

data class GenerateMonthlySavingPlanRequest(
    val budgetLimit: BigDecimal,
    val totalIncome: BigDecimal? = null,
)