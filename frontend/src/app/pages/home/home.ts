import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { UserService } from '../../services/user.service';
import { ComparisonService } from '../../services/comparison.service';
import { ExpensesOverview } from './expenses-overview/expenses-overview';
import { TotalExpenses } from './total-expenses/total-expenses';
import { SpendingTrends } from './spending-trends/spending-trends';
import { RecentTransactions } from './recent-transactions/recent-transactions';
import { TopCategory } from './top-category/top-category';
import { SpendCompare } from './spend-compare/spend-compare';
import { Icon } from '../../shared/icon/icon';

@Component({
  selector: 'app-home',
  imports: [
    ExpensesOverview,
    TotalExpenses,
    SpendingTrends,
    RecentTransactions,
    TopCategory,
    SpendCompare,
    RouterLink,
    Icon,
  ],
  templateUrl: './home.html',
})
export class Home implements OnInit {
  private userService = inject(UserService);
  private comparisonService = inject(ComparisonService);
  private router = inject(Router);

  user = toSignal(this.userService.getCurrentUser());

  hasPreviousMonth = signal(false);
  private readonly thisMonthStart = this.monthStart(0);
  private readonly prevMonthStart = this.monthStart(-1);

  greeting = computed(() => {
    const hour = new Date().getHours();
    if (hour < 12) {
      return 'Good morning';
    }
    if (hour < 18) {
      return 'Good afternoon';
    }
    return 'Good evening';
  });

  ngOnInit(): void {
    this.comparisonService.availablePeriods('MONTH').subscribe((periods) => {
      this.hasPreviousMonth.set(periods.some((p) => p.periodStart === this.prevMonthStart));
    });
  }

  compareWithPreviousMonth(): void {
    this.router.navigate(['/comparison'], {
      queryParams: { type: 'MONTH', current: this.thisMonthStart, previous: this.prevMonthStart, run: 1 },
    });
  }

  private monthStart(offset: number): string {
    const now = new Date();
    const d = new Date(now.getFullYear(), now.getMonth() + offset, 1);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
  }
}
