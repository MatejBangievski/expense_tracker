package mk.sorsix.com.expense_tracker_backend.domain

import mk.sorsix.com.expense_tracker_backend.domain.dto.DailyPlanResponse
import java.math.BigDecimal

sealed class CreateDailyPlanResult {
    data class Success(val dailyPlan: DailyPlanResponse) : CreateDailyPlanResult()
    object PlanNotFound : CreateDailyPlanResult()
    object NotOwner : CreateDailyPlanResult()
    object DateOutsideRange : CreateDailyPlanResult()
    object AlreadyExists : CreateDailyPlanResult()
    data class OverBudgetWarning(val remainingBudget: BigDecimal) : CreateDailyPlanResult()
}

sealed class UpdateDailyPlanResult {
    data class Success(val dailyPlan: DailyPlanResponse) : UpdateDailyPlanResult()
    object DailyPlanNotFound : UpdateDailyPlanResult()
    object NotOwner : UpdateDailyPlanResult()
}

sealed class DeleteDailyPlanResult {
    object Success : DeleteDailyPlanResult()
    object DailyPlanNotFound : DeleteDailyPlanResult()
    object NotOwner : DeleteDailyPlanResult()
}