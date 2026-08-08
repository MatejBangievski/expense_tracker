package mk.sorsix.com.expense_tracker_backend.scheduling

import mk.sorsix.com.expense_tracker_backend.domain.GenerateMonthlySummaryResult
import mk.sorsix.com.expense_tracker_backend.domain.dto.MonthlySummaryRunReport
import mk.sorsix.com.expense_tracker_backend.repository.UserRepository
import mk.sorsix.com.expense_tracker_backend.service.MonthlySummaryService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate


@Component
class MonthlySummaryGenerationJob(
    private val userRepository: UserRepository,
    private val monthlySummaryService: MonthlySummaryService,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${app.scheduling.monthly-summary.cron}", zone = "\${app.scheduling.zone:Europe/Skopje}")
    fun runScheduled() {
        val previousMonth = LocalDate.now(clock).minusMonths(1).withDayOfMonth(1)
        val report = runForAllUsers(previousMonth)
        if (report.failed > 0) {
            log.error(
                "Monthly summary run for {} finished with {} failure(s): {}",
                report.month, report.failed, report.failedUserIds
            )
        }
    }

    fun runForAllUsers(month: LocalDate): MonthlySummaryRunReport {
        val monthStart = month.withDayOfMonth(1)
        val startedAt = System.currentTimeMillis()

        val userIds = userRepository.findAllIds()
        var succeeded = 0
        val failedUserIds = mutableListOf<Long>()

        for (userId in userIds) {
            try {
                when (monthlySummaryService.generateForUser(userId, monthStart)) {
                    is GenerateMonthlySummaryResult.Success -> succeeded++
                    is GenerateMonthlySummaryResult.UserNotFound -> {
                        failedUserIds += userId
                        log.warn("Monthly summary skipped, user no longer exists: userId={} month={}", userId, monthStart)
                    }
                }
            } catch (ex: Exception) {
                failedUserIds += userId
                log.error("Monthly summary generation failed for userId={} month={}", userId, monthStart, ex)
            }
        }

        return MonthlySummaryRunReport(
            month = monthStart,
            usersProcessed = userIds.size,
            succeeded = succeeded,
            failed = failedUserIds.size,
            failedUserIds = failedUserIds,
            durationMillis = System.currentTimeMillis() - startedAt,
        ).also { log.info("Monthly summary run complete: {}", it) }
    }
}