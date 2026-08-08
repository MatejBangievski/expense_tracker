package mk.sorsix.com.expense_tracker_backend.api

import mk.sorsix.com.expense_tracker_backend.domain.GenerateMonthlySavingPlanResult
import mk.sorsix.com.expense_tracker_backend.domain.dto.GenerateMonthlySavingPlanRequest
import mk.sorsix.com.expense_tracker_backend.service.MonthlySavingService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/users/{userId}/monthly-saving-plan")
class MonthlySavingController(
    private val monthlySavingService: MonthlySavingService,
) {

    @PostMapping
    fun generate(@PathVariable userId: Long, @RequestBody request: GenerateMonthlySavingPlanRequest): ResponseEntity<*> =
        when (val result = monthlySavingService.generateWithAIAndPersist(
            userId = userId,
            nextPeriodBudgetLimit = request.budgetLimit,
            totalIncome = request.totalIncome,
        )) {
            is GenerateMonthlySavingPlanResult.Success -> ResponseEntity.ok(result.savingPlan)
            is GenerateMonthlySavingPlanResult.UserNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "User not found"))
            is GenerateMonthlySavingPlanResult.InsufficientData -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(mapOf("error" to result.message))
            is GenerateMonthlySavingPlanResult.AiUnavailable -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("error" to "The recommendation service is unavailable, please try again"))
        }
}