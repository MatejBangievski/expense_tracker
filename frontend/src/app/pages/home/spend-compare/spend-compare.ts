import { Component, computed, inject } from '@angular/core';
import { PercentPipe } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { forkJoin, map } from 'rxjs';
import { ExpenseService } from '../../../services/expense.service';
import { Icon } from '../../../shared/icon/icon';

@Component({
  selector: 'app-spend-compare',
  imports: [PercentPipe, Icon],
  templateUrl: './spend-compare.html',
  styleUrl: './spend-compare.css',
})
export class SpendCompare {
  private service = inject(ExpenseService);

  private data = toSignal(
    forkJoin({
      current: this.service.getTotal(...this.monthRange(0)),
      previous: this.service.getTotal(...this.monthRange(-1)),
    }).pipe(
      map(({ current, previous }) => ({
        loading: false,
        deltaPct: previous > 0 ? (current - previous) / previous : null,
      })),
    ),
    { initialValue: { loading: true, deltaPct: null as number | null } },
  );

  loading = computed(() => this.data().loading);
  deltaPct = computed(() => this.data().deltaPct);
  more = computed(() => (this.deltaPct() ?? 0) > 0);
  deltaAbs = computed(() => Math.abs(this.deltaPct() ?? 0));

  private monthRange(offset: number): [string, string] {
    const now = new Date();
    const start = new Date(now.getFullYear(), now.getMonth() + offset, 1);
    const end = new Date(now.getFullYear(), now.getMonth() + offset + 1, 0);
    return [this.iso(start), this.iso(end)];
  }

  private iso(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
