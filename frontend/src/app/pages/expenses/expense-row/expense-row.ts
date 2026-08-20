import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Expense } from '../../../models/expense';
import { CurrencyPipe } from '@angular/common';

@Component({
  selector: 'app-expense-row',
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './expense-row.html',
  styleUrl: './expense-row.css',
})
export class ExpenseRow {
  expense = input.required<Expense>();
  deleted = output<number>();

  onDelete() {
    this.deleted.emit(this.expense().id);
  }
}
