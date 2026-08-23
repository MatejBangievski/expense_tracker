package mk.sorsix.com.expense_tracker_backend.domain.dto

import java.math.BigDecimal
import java.time.LocalDate

data class DailyPlanRequest(
    val date: LocalDate,
    val allocatedAmount: BigDecimal,
    val confirmOverBudget: Boolean? = false
)

data class DailyPlanResponse(
    val id: Long,
    val planId: Long,
    val date: LocalDate,
    val allocatedAmount: BigDecimal,
    val actualSpent: BigDecimal
)