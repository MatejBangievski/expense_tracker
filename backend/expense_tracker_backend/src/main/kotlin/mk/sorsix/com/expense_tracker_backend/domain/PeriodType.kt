package mk.sorsix.com.expense_tracker_backend.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit


enum class PeriodType {
    WEEK, MONTH, YEAR;

    fun startOf(date: LocalDate): LocalDate = when (this) {
        WEEK -> date.with(DayOfWeek.MONDAY)
        MONTH -> date.withDayOfMonth(1)
        YEAR -> date.withDayOfYear(1)
    }

    fun next(start: LocalDate): LocalDate = when (this) {
        WEEK -> start.plusWeeks(1)
        MONTH -> start.plusMonths(1)
        YEAR -> start.plusYears(1)
    }

    fun previous(start: LocalDate): LocalDate = when (this) {
        WEEK -> start.minusWeeks(1)
        MONTH -> start.minusMonths(1)
        YEAR -> start.minusYears(1)
    }


    fun endOf(start: LocalDate): LocalDate = next(start).minusDays(1)

    fun daysIn(start: LocalDate): Long = ChronoUnit.DAYS.between(start, next(start))
}