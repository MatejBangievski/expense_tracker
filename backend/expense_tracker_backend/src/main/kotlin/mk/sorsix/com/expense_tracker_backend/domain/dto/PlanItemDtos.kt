package mk.sorsix.com.expense_tracker_backend.domain.dto

import java.math.BigDecimal
import java.time.LocalDate

data class CreatePlanItemRequest(
    val categoryId: Long?,
    val description: String,
    val plannedDate: LocalDate,
    val plannedAmount: BigDecimal
)

data class UpdatePlanItemRequest(
    val categoryId: Long?,
    val description: String,
    val plannedDate: LocalDate,
    val plannedAmount: BigDecimal
)

data class PlanItemResponse(
    val id: Long,
    val planId: Long,
    val categoryId: Long?,
    val categoryName: String?,
    val description: String,
    val plannedDate: LocalDate,
    val plannedAmount: BigDecimal
)