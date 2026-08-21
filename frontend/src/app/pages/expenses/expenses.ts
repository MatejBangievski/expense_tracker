import { Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { toSignal, toObservable } from '@angular/core/rxjs-interop';
import { form, FormField } from '@angular/forms/signals';
import { ReplaySubject, debounceTime, distinctUntilChanged, mergeMap, of, switchMap } from 'rxjs';
import { ExpenseService } from '../../services/expense.service';
import { Expense } from '../../models/expense';
import { ExpenseRow } from './expense-row/expense-row';
import { CategoryService } from '../../services/category.service';
import { Category } from '../../models/category';
import { MatFormField } from '@angular/material/form-field';
import {
  MatDatepickerToggle,
  MatDateRangeInput,
  MatDateRangePicker,
  MatStartDate,
  MatEndDate,
  MatDatepickerInputEvent,
} from '@angular/material/datepicker';

interface ExpenseFilterForm {
  q: string;
  categoryName: string;
  periodStart: string;
  periodEnd: string;
}

@Component({
  selector: 'app-expenses',
  imports: [
    FormField,
    RouterLink,
    ExpenseRow,
    MatFormField,
    MatDateRangeInput,
    MatDatepickerToggle,
    MatDateRangePicker,
    MatStartDate,
    MatEndDate,
  ],
  templateUrl: './expenses.html',
  styleUrl: './expenses.css',
})
export class Expenses implements OnInit {
  service = inject(ExpenseService);
  categoryService = inject(CategoryService);

  reload$ = new ReplaySubject<void>();
  expenses = toSignal(
    this.reload$.pipe(mergeMap(() => this.service.getExpensesResult())),
    { initialValue: { data: undefined, loading: true } },
  );

  categories = signal<Category[]>([]);

  filterModel = signal<ExpenseFilterForm>({
    q: '',
    categoryName: '',
    periodStart: '',
    periodEnd: '',
  });
  filterForm = form(this.filterModel);
  filterResult = signal<Expense[]>([]);

  dateRangeLabel = computed(() => {
    const { periodStart, periodEnd } = this.filterModel();
    return periodStart && periodEnd ? `${periodStart} - ${periodEnd}` : '';
  });

  startDateValue = computed(() => {
    const { periodStart } = this.filterModel();
    return periodStart ? new Date(periodStart) : null;
  });

  endDateValue = computed(() => {
    const { periodEnd } = this.filterModel();
    return periodEnd ? new Date(periodEnd) : null;
  });

  private isFilterActive(f: ExpenseFilterForm): boolean {
    return Boolean(f.q || f.categoryName || (f.periodStart && f.periodEnd));
  }

  onStartDateChange(event: MatDatepickerInputEvent<Date>) {
    const start = event.value;
    if (!start) {
      return;
    }
    this.filterModel.update((f) => ({ ...f, periodStart: this.formatDate(start) }));
  }

  onEndDateChange(event: MatDatepickerInputEvent<Date>) {
    const end = event.value;
    if (!end) {
      return;
    }
    this.filterModel.update((f) => ({ ...f, periodEnd: this.formatDate(end) }));
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  constructor() {
    effect(() => {
      if (!this.isFilterActive(this.filterModel())) {
        this.filterResult.set([]);
      }
    });

    toObservable(this.filterModel)
      .pipe(
        debounceTime(400),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        switchMap((f) => {
          if (!this.isFilterActive(f)) {
            return of([]);
          }
          return this.service.getExpenses({
            search: f.q || undefined,
            categoryName: f.categoryName || undefined,
            periodStart: f.periodStart || undefined,
            periodEnd: f.periodEnd || undefined,
          });
        }),
      )
      .subscribe((result) => this.filterResult.set(result));
  }

  ngOnInit(): void {
    this.reload$.next();
    this.categoryService.getCategories().subscribe((categories) => this.categories.set(categories));
  }

  onDelete(id: number) {
    this.service.deleteExpense(id).subscribe(() => this.reload$.next());
  }

  isFiltering(): boolean {
    return this.isFilterActive(this.filterModel());
  }

  resetFilters() {
    this.filterModel.set({ q: '', categoryName: '', periodStart: '', periodEnd: '' });
  }
}
