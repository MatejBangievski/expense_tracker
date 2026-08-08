package mk.sorsix.com.expense_tracker_backend.scheduling

import java.time.LocalDate

data class MonthlySummaryRunReport(
    val month: LocalDate,
    val usersProcessed: Int,
    val succeeded: Int,
    val failed: Int,
    val failedUserIds: List<Long>,
    val durationMillis: Long,
)