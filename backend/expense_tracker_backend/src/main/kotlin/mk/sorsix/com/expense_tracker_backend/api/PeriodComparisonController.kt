package mk.sorsix.com.expense_tracker_backend.api

import mk.sorsix.com.expense_tracker_backend.domain.GeneratePeriodComparisonResult
import mk.sorsix.com.expense_tracker_backend.domain.PeriodType
import mk.sorsix.com.expense_tracker_backend.domain.dto.ComparePeriodsRequest
import mk.sorsix.com.expense_tracker_backend.security.CurrentUserProvider
import mk.sorsix.com.expense_tracker_backend.service.PeriodComparisonService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("api/period-comparisons")
class PeriodComparisonController(
    private val periodComparisonService: PeriodComparisonService,
    private val currentUserProvider: CurrentUserProvider,
) {

    @PostMapping("/previous")
    fun compareWithPreviousPeriod(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam(defaultValue = "MONTH") periodType: PeriodType,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
    ): ResponseEntity<*> =
        periodComparisonService.compareWithPreviousPeriod(currentUserProvider.resolve(userDetails), periodType, date)
            .toResponseEntity()

    @PostMapping
    fun compare(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody request: ComparePeriodsRequest,
    ): ResponseEntity<*> =
        periodComparisonService.compare(
            currentUserProvider.resolve(userDetails),
            request.currentPeriodType, request.currentDate,
            request.previousPeriodType, request.previousDate,
        ).toResponseEntity()

    private fun GeneratePeriodComparisonResult.toResponseEntity(): ResponseEntity<*> = when (this) {
        is GeneratePeriodComparisonResult.Success -> ResponseEntity.ok(comparison)
        is GeneratePeriodComparisonResult.SamePeriod -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to "Cannot compare a period with itself"))
        is GeneratePeriodComparisonResult.InadequatePeriods -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to "Periods must be of the same type (WEEK, MONTH or YEAR)"))
        is GeneratePeriodComparisonResult.InsufficientData -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(mapOf("error" to message))
        is GeneratePeriodComparisonResult.AiUnavailable -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(mapOf("error" to "The comparison service is unavailable, please try again"))
    }
}