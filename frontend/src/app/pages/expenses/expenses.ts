import { Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal, toObservable } from '@angular/core/rxjs-interop';
import { form, FormField } from '@angular/forms/signals';
import { ReplaySubject, debounceTime, distinctUntilChanged, mergeMap, of, switchMap } from 'rxjs';
import { ExpenseService } from '../../services/expense.service';
import { Expense } from '../../models/expense';
import { ExpenseRow } from './expense-row/expense-row';
import { CategoryService } from '../../services/category.service';
import { Category } from '../../models/category';
import { Spinner } from '../../shared/spinner/spinner';
import { MatFormField, MatLabel } from '@angular/material/form-field';
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
    Spinner,
    MatFormField,
    MatLabel,
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
  route = inject(ActivatedRoute);

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

  loading = computed(() => !this.isFiltering() && this.expenses().loading);

  displayedExpenses = computed(() => {
    const list = this.isFiltering() ? this.filterResult() : (this.expenses().data ?? []);
    return [...list].sort((a, b) => b.expenseDate.localeCompare(a.expenseDate) || b.id - a.id);
  });

  startDateValue = computed(() => {
    const { periodStart } = this.filterModel();
    return periodStart ? new Date(periodStart) : null;
  });

  endDateValue = computed(() => {
    const { periodEnd } = this.filterModel();
    return periodEnd ? new Date(periodEnd) : null;
  });

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

  private isFilterActive(f: ExpenseFilterForm): boolean {
    return Boolean(f.q || f.categoryName || (f.periodStart && f.periodEnd));
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

    const categoryName = this.route.snapshot.queryParamMap.get('categoryName');
    if (categoryName) {
      this.filterModel.update((f) => ({ ...f, categoryName }));
    }
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
