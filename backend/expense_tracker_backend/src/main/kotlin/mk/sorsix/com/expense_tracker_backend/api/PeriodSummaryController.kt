package mk.sorsix.com.expense_tracker_backend.api

import mk.sorsix.com.expense_tracker_backend.domain.AvailablePeriodsResult
import mk.sorsix.com.expense_tracker_backend.domain.CurrentSpendingResult
import mk.sorsix.com.expense_tracker_backend.domain.PeriodType
import mk.sorsix.com.expense_tracker_backend.security.CurrentUserProvider
import mk.sorsix.com.expense_tracker_backend.service.PeriodSummaryService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/period-summaries")
class PeriodSummaryController(
    private val periodSummaryService: PeriodSummaryService,
    private val currentUserProvider: CurrentUserProvider,
) {

    @GetMapping("/current")
    fun current(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<*> =
        when (val result = periodSummaryService.currentSpending(currentUserProvider.resolve(userDetails).id)) {
            is CurrentSpendingResult.Success -> ResponseEntity.ok(result.summary)
            is CurrentSpendingResult.UserNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "User not found"))
        }

    @GetMapping
    fun periods(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam(defaultValue = "MONTH") periodType: PeriodType,
    ): ResponseEntity<*> =
        when (val result = periodSummaryService.availablePeriods(currentUserProvider.resolve(userDetails).id, periodType)) {
            is AvailablePeriodsResult.Success -> ResponseEntity.ok(result.periods)
            is AvailablePeriodsResult.UserNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "User not found"))
        }
}
