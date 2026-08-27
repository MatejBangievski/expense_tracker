package mk.sorsix.com.expense_tracker_backend.api

import mk.sorsix.com.expense_tracker_backend.domain.GenerateMonthlySavingPlanResult
import mk.sorsix.com.expense_tracker_backend.domain.dto.GenerateMonthlySavingPlanRequest
import mk.sorsix.com.expense_tracker_backend.domain.dto.ManualSavingPlanRequest
import mk.sorsix.com.expense_tracker_backend.security.CurrentUserProvider
import mk.sorsix.com.expense_tracker_backend.service.MonthlySavingService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/monthly-saving-plan")
class MonthlySavingController(
    private val monthlySavingService: MonthlySavingService,
    private val currentUserProvider: CurrentUserProvider,
) {

    @GetMapping
    fun current(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<*> =
        monthlySavingService.getCurrentPlan(currentUserProvider.resolve(userDetails))
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "No saving plan for this month"))

    @PostMapping
    fun createManual(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody request: ManualSavingPlanRequest,
    ): ResponseEntity<*> =
        monthlySavingService.createManualPlan(currentUserProvider.resolve(userDetails), request).toResponse()

    @PostMapping("/ai")
    fun generate(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody request: GenerateMonthlySavingPlanRequest,
    ): ResponseEntity<*> =
        monthlySavingService.generateWithAIAndPersist(
            user = currentUserProvider.resolve(userDetails),
            budgetLimit = request.budgetLimit,
            totalIncome = request.totalIncome,
        ).toResponse()

    private fun GenerateMonthlySavingPlanResult.toResponse(): ResponseEntity<*> = when (this) {
        is GenerateMonthlySavingPlanResult.Success -> ResponseEntity.ok(savingPlan)
        is GenerateMonthlySavingPlanResult.InsufficientData -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(mapOf("error" to message))
        is GenerateMonthlySavingPlanResult.AiUnavailable -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(mapOf("error" to "The recommendation service is unavailable, please try again"))
    }
}