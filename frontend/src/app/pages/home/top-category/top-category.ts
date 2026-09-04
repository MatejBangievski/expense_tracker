import { Component, computed, inject } from '@angular/core';
import { CurrencyPipe, PercentPipe } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { ExpenseService } from '../../../services/expense.service';
import { Icon } from '../../../shared/icon/icon';
import { categoryColor, categoryIcon } from '../../../shared/category-icons';
import { Spinner } from '../../../shared/spinner/spinner';

@Component({
  selector: 'app-top-category',
  imports: [CurrencyPipe, PercentPipe, Icon, Spinner],
  templateUrl: './top-category.html',
  styleUrl: './top-category.css',
})
export class TopCategory {
  private service = inject(ExpenseService);

  private data = toSignal(this.service.getTopCategory(this.monthStart(), this.today()), {
    initialValue: undefined,
  });

  loading = computed(() => this.data() === undefined);

  top = computed(() => {
    const t = this.data();
    if (!t) {
      return null;
    }
    return {
      name: t.categoryName,
      spent: t.totalSpent,
      share: t.share,
      icon: categoryIcon(t.categoryName),
      color: categoryColor(t.categoryName),
    };
  });

  private monthStart(): string {
    const now = new Date();
    return this.iso(new Date(now.getFullYear(), now.getMonth(), 1));
  }

  private today(): string {
    return this.iso(new Date());
  }

  private iso(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
