package mk.sorsix.com.expense_tracker_backend.scheduling

import mk.sorsix.com.expense_tracker_backend.domain.GeneratePeriodSummaryResult
import mk.sorsix.com.expense_tracker_backend.domain.PeriodType
import mk.sorsix.com.expense_tracker_backend.domain.dto.PeriodSummaryRunReport
import mk.sorsix.com.expense_tracker_backend.repository.UserRepository
import mk.sorsix.com.expense_tracker_backend.service.PeriodSummaryService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate

@Component
class PeriodSummaryGenerationJob(
    private val userRepository: UserRepository,
    private val periodSummaryService: PeriodSummaryService,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${app.scheduling.week-summary.cron}", zone = "\${app.scheduling.zone:Europe/Skopje}")
    fun runWeekly() = runForPreviousPeriod(PeriodType.WEEK)

    @Scheduled(cron = "\${app.scheduling.month-summary.cron}", zone = "\${app.scheduling.zone:Europe/Skopje}")
    fun runMonthly() = runForPreviousPeriod(PeriodType.MONTH)

    @Scheduled(cron = "\${app.scheduling.year-summary.cron}", zone = "\${app.scheduling.zone:Europe/Skopje}")
    fun runYearly() = runForPreviousPeriod(PeriodType.YEAR)

    private fun runForPreviousPeriod(periodType: PeriodType) {
        val previousStart = periodType.previous(periodType.startOf(LocalDate.now(clock)))
        val report = runForAllUsers(periodType, previousStart)
        if (report.failed > 0) {
            log.error(
                "{} summary run for {} finished with {} failure(s): {}",
                periodType,
                report.periodStart,
                report.failed,
                report.failedUserIds,
            )
        }
    }

    fun runForAllUsers(
        periodType: PeriodType,
        periodStart: LocalDate,
    ): PeriodSummaryRunReport {
        val normalizedStart = periodType.startOf(periodStart)
        val startedAt = System.currentTimeMillis()

        val userIds = userRepository.findAllIds()
        var succeeded = 0
        val failedUserIds = mutableListOf<Long>()

        for (userId in userIds) {
            try {
                when (periodSummaryService.generateForUser(userId, periodType, normalizedStart)) {
                    is GeneratePeriodSummaryResult.Success -> succeeded++
                    is GeneratePeriodSummaryResult.UserNotFound -> {
                        failedUserIds += userId
                        log.warn("{} summary skipped, user no longer exists: userId={} periodStart={}", periodType, userId, normalizedStart)
                    }
                }
            } catch (ex: Exception) {
                failedUserIds += userId
                log.error("{} summary generation failed for userId={} periodStart={}", periodType, userId, normalizedStart, ex)
            }
        }

        return PeriodSummaryRunReport(
            periodType = periodType,
            periodStart = normalizedStart,
            usersProcessed = userIds.size,
            succeeded = succeeded,
            failed = failedUserIds.size,
            failedUserIds = failedUserIds,
            durationMillis = System.currentTimeMillis() - startedAt,
        ).also { log.info("{} summary run complete: {}", periodType, it) }
    }
}
