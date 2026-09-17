import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Result } from '../models/result';
import { Expense, ExpenseFilter, TopCategory } from '../models/expense';

@Service()
export class ExpenseService {
  http = inject(HttpClient);

  getExpenses(filter: ExpenseFilter = {}): Observable<Expense[]> {
    return this.http.get<Expense[]>(`/api/expenses`, { params: this.buildParams(filter) });
  }

  getExpensesResult(filter: ExpenseFilter = {}): Observable<Result<Expense[]>> {
    return this.http
      .get<Expense[]>(`/api/expenses`, { params: this.buildParams(filter) })
      .pipe(map((result) => ({ data: result, loading: false })));
  }

  getExpenseById(id: number): Observable<Expense | undefined> {
    return this.http.get<Expense>(`/api/expenses/${id}`);
  }

  getRecent(limit: number): Observable<Expense[]> {
    return this.http.get<Expense[]>(`/api/expenses/recent`, { params: { limit } });
  }

  getTopCategory(periodStart: string, periodEnd: string): Observable<TopCategory | null> {
    return this.http.get<TopCategory | null>(`/api/expenses/top-category`, {
      params: { periodStart, periodEnd },
    });
  }

  getTotal(periodStart: string, periodEnd: string): Observable<number> {
    return this.http
      .get<{ total: number }>(`/api/expenses/total`, { params: { periodStart, periodEnd } })
      .pipe(map((response) => response.total));
  }

  save(expense: {
    categoryId: number;
    amount: number;
    expenseDate: string;
    description: string;
    confirmOverBudget?: boolean;
  }) {
    return this.http.post<Expense>(`/api/expenses`, expense);
  }

  update(
    id: number,
    expense: {
      categoryId: number;
      amount: number;
      expenseDate: string;
      description: string;
      confirmOverBudget?: boolean;
    },
  ) {
    return this.http.put<Expense>(`/api/expenses/${id}`, expense);
  }

  deleteExpense(id: number) {
    return this.http.delete(`/api/expenses/${id}`);
  }

  private buildParams(filter: ExpenseFilter = {}): Record<string, string> {
    const params: Record<string, string> = {};

    if (filter.search) {
      params['search'] = filter.search;
    }

    if (filter.categoryName) {
      params['categoryName'] = filter.categoryName;
    }

    if (filter.periodStart) {
      params['periodStart'] = filter.periodStart;
    }

    if (filter.periodEnd) {
      params['periodEnd'] = filter.periodEnd;
    }

    return params;
  }
}
