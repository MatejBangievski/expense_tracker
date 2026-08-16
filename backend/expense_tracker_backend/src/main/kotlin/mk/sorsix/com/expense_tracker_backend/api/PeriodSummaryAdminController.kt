package mk.sorsix.com.expense_tracker_backend.api

import mk.sorsix.com.expense_tracker_backend.domain.FindPeriodSummaryResult
import mk.sorsix.com.expense_tracker_backend.domain.GeneratePeriodSummaryResult
import mk.sorsix.com.expense_tracker_backend.domain.PeriodType
import mk.sorsix.com.expense_tracker_backend.domain.dto.PeriodSummaryRunReport
import mk.sorsix.com.expense_tracker_backend.scheduling.PeriodSummaryGenerationJob
import mk.sorsix.com.expense_tracker_backend.service.PeriodSummaryService
import org.springframework.context.annotation.Profile
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.LocalDate

@RestController
@RequestMapping("api/admin/period-summaries")
@Profile("dev")
class PeriodSummaryAdminController(
    private val job: PeriodSummaryGenerationJob,
    private val periodSummaryService: PeriodSummaryService,
    private val clock: Clock,
) {

    /** POST /api/admin/period-summaries/run?periodType=MONTH&date=2026-07-01 */
    @PostMapping("/run")
    fun runForAllUsers(
        @RequestParam(defaultValue = "MONTH") periodType: PeriodType,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
    ): PeriodSummaryRunReport = job.runForAllUsers(periodType, defaultStart(periodType, date))

    /** POST /api/admin/period-summaries/users/1?periodType=MONTH&date=2026-07-01 */
    @PostMapping("/users/{userId}")
    fun runForUser(
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "MONTH") periodType: PeriodType,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
    ): ResponseEntity<*> =
        when (val result = periodSummaryService.generateForUser(userId, periodType, defaultStart(periodType, date))) {
            is GeneratePeriodSummaryResult.Success -> ResponseEntity.ok(result.summary)
            is GeneratePeriodSummaryResult.UserNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "User not found"))
        }

    /** GET /api/admin/period-summaries/users/1?periodType=MONTH&date=2026-07-01 — inspect without regenerating. */
    @GetMapping("/users/{userId}")
    fun get(
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "MONTH") periodType: PeriodType,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
    ): ResponseEntity<*> =
        when (val result = periodSummaryService.find(userId, periodType, defaultStart(periodType, date))) {
            is FindPeriodSummaryResult.Success -> ResponseEntity.ok(result.summary)
            is FindPeriodSummaryResult.SummaryNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "No summary for this user, period type and period"))
        }

    /** Defaults to the previous period of the requested type when no date is supplied. */
    private fun defaultStart(periodType: PeriodType, date: LocalDate?): LocalDate =
        date ?: periodType.previous(periodType.startOf(LocalDate.now(clock)))
}