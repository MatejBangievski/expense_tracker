package mk.sorsix.com.expense_tracker_backend.controller

import mk.sorsix.com.expense_tracker_backend.api.GenerateMonthlySavingPlanRequest
import mk.sorsix.com.expense_tracker_backend.api.GeminiApiResult
import mk.sorsix.com.expense_tracker_backend.service.MonthlySavingService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users/{userId}/monthly-saving-plan")
class MonthlySavingController(
    private val monthlySavingService: MonthlySavingService,
) {

    @PostMapping
    fun generate(@PathVariable userId: Long, @RequestBody request: GenerateMonthlySavingPlanRequest): GeminiApiResult =
        monthlySavingService.generateWithAIAndPersist(
            userId = userId,
            nextPeriodBudgetLimit = request.budgetLimit,
            totalIncome = request.totalIncome,
        )
}