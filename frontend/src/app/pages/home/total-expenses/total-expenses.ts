import { Component, computed, inject } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { forkJoin, map } from 'rxjs';
import { ExpenseService } from '../../../services/expense.service';
import { Expense, ExpenseFilter } from '../../../models/expense';
import { Icon } from '../../../shared/icon/icon';

@Component({
  selector: 'app-total-expenses',
  imports: [CurrencyPipe, DecimalPipe, Icon],
  templateUrl: './total-expenses.html',
  styleUrl: './total-expenses.css',
})
export class TotalExpenses {
  private service = inject(ExpenseService);

  private data = toSignal(
    forkJoin({
      current: this.service.getExpenses(this.weekRange(0)),
      previous: this.service.getExpenses(this.weekRange(-1)),
    }).pipe(
      map(({ current, previous }) => {
        const currentTotal = this.sum(current);
        const previousTotal = this.sum(previous);
        return {
          loading: false,
          total: currentTotal,
          delta: previousTotal > 0 ? (currentTotal - previousTotal) / previousTotal : null,
        };
      }),
    ),
    { initialValue: { loading: true, total: 0, delta: null as number | null } },
  );

  loading = computed(() => this.data().loading);
  total = computed(() => this.data().total);
  delta = computed(() => this.data().delta);
  deltaUp = computed(() => (this.delta() ?? 0) > 0);
  deltaAbs = computed(() => Math.abs((this.delta() ?? 0) * 100));

  private sum(expenses: Expense[]): number {
    return expenses.reduce((total, expense) => total + expense.amount, 0);
  }

  private weekRange(weekOffset: number): ExpenseFilter {
    const end = new Date();
    end.setDate(end.getDate() + weekOffset * 7);
    const start = new Date(end);
    start.setDate(end.getDate() - 6);
    return { periodStart: this.iso(start), periodEnd: this.iso(end) };
  }

  private iso(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
