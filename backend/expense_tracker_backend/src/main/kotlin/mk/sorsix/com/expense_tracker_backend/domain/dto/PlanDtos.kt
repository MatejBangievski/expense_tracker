package mk.sorsix.com.expense_tracker_backend.domain.dto
import java.math.BigDecimal
import java.time.LocalDate

data class CreatePlanRequest(
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalBudget: BigDecimal,
)

data class UpdatePlanRequest(
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalBudget: BigDecimal,
)

data class PlanResponse(
    val id: Long,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalBudget: BigDecimal,
    val totalPlanned: BigDecimal,
)
