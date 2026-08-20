import { Component, effect, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { toSignal, toObservable } from '@angular/core/rxjs-interop';
import { form, FormField } from '@angular/forms/signals';
import { ReplaySubject, debounceTime, distinctUntilChanged, filter, map, mergeMap, switchMap } from 'rxjs';
import { ExpenseService } from '../../services/expense.service';
import { Expense } from '../../models/expense';
import { ExpenseRow } from './expense-row/expense-row';

@Component({
  selector: 'app-expenses',
  imports: [FormField, RouterLink,ExpenseRow],
  templateUrl: './expenses.html',
  styleUrl: './expenses.css'
})
export class Expenses implements OnInit {
  service = inject(ExpenseService);


  reload$ = new ReplaySubject<void>();
  expenses = toSignal(
    this.reload$.pipe(mergeMap(() => this.service.getExpensesResult())),
    { initialValue: { data: undefined, loading: true } }
  );


  query = signal<{ q: string }>({ q: '' });
  searchForm = form(this.query);
  searchResult = signal<Expense[]>([]);

  constructor() {
    effect(() => {
      if (this.query().q === '') {
        this.searchResult.set([]);
      }
    });

    toObservable(this.query)
      .pipe(
        filter(({ q }) => Boolean(q)),
        map((query) => query.q),
        debounceTime(400),
        distinctUntilChanged(),
        switchMap((q) => this.service.getExpenses({ search: q }))
      )
      .subscribe((result) => this.searchResult.set(result));
  }

  ngOnInit(): void {
    this.reload$.next();
  }

  onDelete(id: number) {
    this.service.deleteExpense(id).subscribe(() => this.reload$.next());
  }
}
