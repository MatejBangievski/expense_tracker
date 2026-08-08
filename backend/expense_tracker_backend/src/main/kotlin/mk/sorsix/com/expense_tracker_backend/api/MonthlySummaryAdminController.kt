package mk.sorsix.com.expense_tracker_backend.api

import mk.sorsix.com.expense_tracker_backend.domain.dto.MonthlySummaryView
import mk.sorsix.com.expense_tracker_backend.scheduling.MonthlySummaryGenerationJob
import mk.sorsix.com.expense_tracker_backend.scheduling.MonthlySummaryRunReport
import mk.sorsix.com.expense_tracker_backend.service.MonthlySummaryService
import org.springframework.context.annotation.Profile
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.LocalDate

@RestController
@RequestMapping("/api/admin/monthly-summaries")
@Profile("dev")
class MonthlySummaryAdminController(
    private val job: MonthlySummaryGenerationJob,
    private val monthlySummaryService: MonthlySummaryService,
    private val clock: Clock,
) {

    /** POST /api/admin/monthly-summaries/run?month=2026-07-01 */
    @PostMapping("/run")
    fun runForAllUsers(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) month: LocalDate?,
    ): MonthlySummaryRunReport = job.runForAllUsers(month ?: defaultMonth())

    /** POST /api/admin/monthly-summaries/users/1?month=2026-07-01 */
    @PostMapping("/users/{userId}")
    fun runForUser(
        @PathVariable userId: Long,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) month: LocalDate?,
    ): MonthlySummaryView = monthlySummaryService.generateForUser(userId, month ?: defaultMonth())

    /** GET /api/admin/monthly-summaries/users/1?month=2026-07-01 — inspect without regenerating. */
    @GetMapping("/users/{userId}")
    fun get(
        @PathVariable userId: Long,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) month: LocalDate?,
    ): MonthlySummaryView =
        monthlySummaryService.find(userId, month ?: defaultMonth())
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

    private fun defaultMonth(): LocalDate = LocalDate.now(clock).minusMonths(1).withDayOfMonth(1)
}