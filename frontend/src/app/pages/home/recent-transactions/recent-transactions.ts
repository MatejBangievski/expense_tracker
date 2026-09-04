import { Component, computed, inject } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { ExpenseService } from '../../../services/expense.service';
import { Expense } from '../../../models/expense';
import { Icon } from '../../../shared/icon/icon';
import { categoryColor, categoryIcon } from '../../../shared/category-icons';
import { Spinner } from '../../../shared/spinner/spinner';

@Component({
  selector: 'app-recent-transactions',
  imports: [CurrencyPipe, DatePipe, RouterLink, Icon, Spinner],
  templateUrl: './recent-transactions.html',
  styleUrl: './recent-transactions.css',
})
export class RecentTransactions {
  private service = inject(ExpenseService);

  private data = toSignal(this.service.getRecent(7), { initialValue: undefined });

  loading = computed(() => this.data() === undefined);

  recent = computed(() => this.data() ?? []);

  icon(expense: Expense): string {
    return categoryIcon(expense.categoryName);
  }

  color(expense: Expense): string {
    return categoryColor(expense.categoryName);
  }
}
