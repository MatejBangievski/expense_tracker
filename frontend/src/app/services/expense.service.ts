import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Result } from '../models/result';
import { Expense, ExpenseFilter } from '../models/expense';

@Service()
export class ExpenseService {
  http = inject(HttpClient);

  getExpenses(filter: ExpenseFilter = {}): Observable<Expense[]> {
    return this.http.get<Expense[]>(`/api/expenses`, { params: { ...filter } });
  }

  getExpensesResult(filter: ExpenseFilter = {}): Observable<Result<Expense[]>> {
    return this.http.get<Expense[]>(`/api/expenses`, { params: { ...filter } }).pipe(
      map((result) => ({ data: result, loading: false })),
    );
  }

  getExpenseById(id: number): Observable<Expense | undefined> {
    return this.http.get<Expense>(`/api/expenses/${id}`);
  }

  save(expense: { categoryId: number; amount: number; expenseDate: string; description: string }) {
    return this.http.post(`/api/expenses`, expense);
  }

  update(id: number, expense: { categoryId: number; amount: number; expenseDate: string; description: string }) {
    return this.http.put(`/api/expenses/${id}`, expense);
  }

  deleteExpense(id: number) {
    return this.http.delete(`/api/expenses/${id}`);
  }
}
