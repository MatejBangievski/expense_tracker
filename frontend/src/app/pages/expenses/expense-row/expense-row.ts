import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Expense } from '../../../models/expense';
import { CurrencyPipe, DatePipe } from '@angular/common';

@Component({
  selector: 'tr[app-expense-row]',
  imports: [RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './expense-row.html',
})
export class ExpenseRow {
  expense = input.required<Expense>();
  deleted = output<number>();

  onDelete() {
    this.deleted.emit(this.expense().id);
  }
}
