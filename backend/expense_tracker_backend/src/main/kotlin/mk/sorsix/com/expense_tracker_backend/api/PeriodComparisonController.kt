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
    @PostMapping
    fun compare(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody request: ComparePeriodsRequest,
        @RequestParam(defaultValue = "false") useAi: Boolean,
    ): ResponseEntity<*> =
        when (val result = periodComparisonService.compare(
            currentUserProvider.resolve(userDetails),
            request.currentPeriodType, request.currentDate,
            request.previousPeriodType, request.previousDate, useAi,
        )) {
            is GeneratePeriodComparisonResult.Success -> ResponseEntity.ok(result.comparison)
            is GeneratePeriodComparisonResult.SamePeriod -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Cannot compare a period with itself"))

            is GeneratePeriodComparisonResult.InadequatePeriods -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Periods must be of the same type (WEEK, MONTH or YEAR)"))
        }

    @PostMapping("/previous")
    fun compareWithPreviousPeriod(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam(defaultValue = "MONTH") periodType: PeriodType,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
        @RequestParam(defaultValue = "false") useAi: Boolean,
    ): ResponseEntity<*> =
        when (val result = periodComparisonService.compareWithPreviousPeriod(
            currentUserProvider.resolve(userDetails), periodType, date, useAi,
        )) {
            is GeneratePeriodComparisonResult.Success -> ResponseEntity.ok(result.comparison)
            is GeneratePeriodComparisonResult.SamePeriod -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Cannot compare a period with itself"))

            is GeneratePeriodComparisonResult.InadequatePeriods -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Periods must be of the same type (WEEK, MONTH or YEAR)"))
        }
}