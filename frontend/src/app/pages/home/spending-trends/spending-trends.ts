import { Component, computed, inject, signal } from '@angular/core';
import { toSignal, toObservable } from '@angular/core/rxjs-interop';
import { switchMap } from 'rxjs';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';
import { ExpenseService } from '../../../services/expense.service';
import { Expense, ExpenseFilter } from '../../../models/expense';
import { Spinner } from '../../../shared/spinner/spinner';

type TrendPeriod = 'month' | 'last30' | 'year';

interface RangeInfo {
  start: Date;
  end: Date;
  granularity: 'day' | 'month';
  filter: ExpenseFilter;
}

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

@Component({
  selector: 'app-spending-trends',
  imports: [BaseChartDirective, Spinner],
  templateUrl: './spending-trends.html',
  styleUrl: './spending-trends.css',
})
export class SpendingTrends {
  private service = inject(ExpenseService);

  period = signal<TrendPeriod>('month');

  private rangeInfo = computed(() => this.rangeFor(this.period()));

  private expenses = toSignal(
    toObservable(computed(() => this.rangeInfo().filter)).pipe(
      switchMap((filter) => this.service.getExpensesResult(filter)),
    ),
    { initialValue: { data: undefined, loading: true } },
  );

  loading = computed(() => this.expenses().loading);

  private buckets = computed(() => this.buildBuckets(this.expenses().data ?? [], this.rangeInfo()));

  chartData = computed<ChartData<'line'>>(() => ({
    labels: this.buckets().labels,
    datasets: [
      {
        data: this.buckets().data,
        fill: true,
        borderColor: '#3b82f6',
        backgroundColor: 'rgba(59, 130, 246, 0.15)',
        borderWidth: 2,
        tension: 0.35,
        pointRadius: 2,
        pointBackgroundColor: '#3b82f6',
      },
    ],
  }));

  chartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: (ctx) => '$' + Number(ctx.parsed.y).toFixed(2),
        },
      },
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: { callback: (value) => '$' + value },
      },
      x: {
        grid: { display: false },
      },
    },
  };

  setPeriod(period: TrendPeriod) {
    this.period.set(period);
  }

  private buildBuckets(
    expenses: Expense[],
    range: RangeInfo,
  ): { labels: string[]; data: number[] } {
    if (range.granularity === 'month') {
      const labels: string[] = [];
      const data: number[] = [];
      for (let month = 0; month <= range.end.getMonth(); month++) {
        const prefix = `${range.end.getFullYear()}-${String(month + 1).padStart(2, '0')}`;
        labels.push(MONTHS[month]);
        data.push(this.sumWhere(expenses, (date) => date.startsWith(prefix)));
      }
      return { labels, data };
    }

    const labels: string[] = [];
    const data: number[] = [];
    const cursor = new Date(range.start);
    while (cursor <= range.end) {
      const iso = this.iso(cursor);
      labels.push(`${MONTHS[cursor.getMonth()]} ${cursor.getDate()}`);
      data.push(this.sumWhere(expenses, (date) => date === iso));
      cursor.setDate(cursor.getDate() + 1);
    }
    return { labels, data };
  }

  private sumWhere(expenses: Expense[], predicate: (date: string) => boolean): number {
    return expenses.reduce(
      (total, expense) => (predicate(expense.expenseDate) ? total + expense.amount : total),
      0,
    );
  }

  private rangeFor(period: TrendPeriod): RangeInfo {
    const now = new Date();
    if (period === 'year') {
      const start = new Date(now.getFullYear(), 0, 1);
      const end = new Date(now.getFullYear(), 11, 31);
      return {
        start,
        end,
        granularity: 'month',
        filter: { periodStart: this.iso(start), periodEnd: this.iso(end) },
      };
    }
    if (period === 'last30') {
      const end = new Date(now);
      const start = new Date(now);
      start.setDate(now.getDate() - 29);
      return {
        start,
        end,
        granularity: 'day',
        filter: { periodStart: this.iso(start), periodEnd: this.iso(end) },
      };
    }
    const start = new Date(now.getFullYear(), now.getMonth(), 1);
    const end = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    return {
      start,
      end,
      granularity: 'day',
      filter: { periodStart: this.iso(start), periodEnd: this.iso(end) },
    };
  }

  private iso(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
