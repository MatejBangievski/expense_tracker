import { Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, PercentPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { toSignal, toObservable } from '@angular/core/rxjs-interop';
import { switchMap } from 'rxjs';
import { BaseChartDirective } from 'ng2-charts';
import { ActiveElement, ChartData, ChartEvent, ChartOptions } from 'chart.js';
import { ExpenseService } from '../../../services/expense.service';
import { Expense, ExpenseFilter } from '../../../models/expense';
import { Spinner } from '../../../shared/spinner/spinner';

type Period = 'weekly' | 'monthly' | 'yearly';

interface CategorySegment {
  name: string;
  amount: number;
  share: number;
  color: string;
}

const SEGMENT_COLORS = ['#3b82f6', '#34c77b', '#f5c451', '#ef5b5b', '#a78bfa'];
const OTHERS_COLOR = '#cbd5e1';

@Component({
  selector: 'app-expenses-overview',
  imports: [CurrencyPipe, PercentPipe, RouterLink, BaseChartDirective, Spinner],
  templateUrl: './expenses-overview.html',
  styleUrl: './expenses-overview.css',
})
export class ExpensesOverview {
  private service = inject(ExpenseService);
  private router = inject(Router);

  period = signal<Period>('weekly');

  private range = computed(() => this.rangeFor(this.period()));

  private expenses = toSignal(
    toObservable(this.range).pipe(switchMap((range) => this.service.getExpensesResult(range))),
    { initialValue: { data: undefined, loading: true } },
  );

  loading = computed(() => this.expenses().loading);
  segments = computed(() => this.buildSegments(this.expenses().data ?? []));
  total = computed(() => this.segments().reduce((sum, segment) => sum + segment.amount, 0));

  chartData = computed<ChartData<'doughnut'>>(() => ({
    labels: this.segments().map((segment) => segment.name),
    datasets: [
      {
        data: this.segments().map((segment) => segment.amount),
        backgroundColor: this.segments().map((segment) => segment.color),
        borderWidth: 0,
      },
    ],
  }));

  chartOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '70%',
    plugins: {
      legend: { display: false },
    },
    onClick: (_event: ChartEvent, elements: ActiveElement[]) => this.onSegmentClick(elements),
  };

  setPeriod(period: Period) {
    this.period.set(period);
  }

  private onSegmentClick(elements: ActiveElement[]) {
    if (elements.length === 0) {
      return;
    }
    const segment = this.segments()[elements[0].index];
    if (!segment || segment.name === 'Others') {
      return;
    }
    this.router.navigate(['/expenses'], { queryParams: { categoryName: segment.name } });
  }

  private buildSegments(expenses: Expense[]): CategorySegment[] {
    const totals = new Map<string, number>();
    for (const expense of expenses) {
      totals.set(expense.categoryName, (totals.get(expense.categoryName) ?? 0) + expense.amount);
    }

    const sorted = [...totals.entries()].sort((a, b) => b[1] - a[1]);
    const total = sorted.reduce((sum, [, amount]) => sum + amount, 0);
    if (total === 0) {
      return [];
    }

    const top = sorted.slice(0, SEGMENT_COLORS.length);
    const rest = sorted.slice(SEGMENT_COLORS.length);

    const segments: CategorySegment[] = top.map(([name, amount], index) => ({
      name,
      amount,
      share: amount / total,
      color: SEGMENT_COLORS[index],
    }));

    const othersAmount = rest.reduce((sum, [, amount]) => sum + amount, 0);
    if (othersAmount > 0) {
      segments.push({ name: 'Others', amount: othersAmount, share: othersAmount / total, color: OTHERS_COLOR });
    }

    return segments;
  }

  private rangeFor(period: Period): ExpenseFilter {
    const end = new Date();
    const start = new Date(end);
    if (period === 'weekly') {
      start.setDate(end.getDate() - 6);
    } else if (period === 'monthly') {
      start.setDate(1);
    } else {
      start.setMonth(0, 1);
    }
    return { periodStart: this.iso(start), periodEnd: this.iso(end) };
  }

  private iso(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
