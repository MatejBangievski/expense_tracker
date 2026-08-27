import { Component, inject, OnInit, signal } from '@angular/core';
import { form, FormField, FormRoot, min, required } from '@angular/forms/signals';
import { HttpErrorResponse } from '@angular/common/http';
import { ExpenseService } from '../../../services/expense.service';
import { CategoryService } from '../../../services/category.service';
import { firstValueFrom, map, mergeMap, of } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { Expense } from '../../../models/expense';
import { Category } from '../../../models/category';
import { PlanService } from '../../../services/plan.service';
import { Plan } from '../../../models/plan';

@Component({
  selector: 'app-expense-form',
  imports: [FormField, FormRoot],
  templateUrl: './expense-form.html',
  styleUrl: './expense-form.css',
})
export class ExpenseForm implements OnInit {
  service = inject(ExpenseService);
  categoryService = inject(CategoryService);
  planService = inject(PlanService);
  router = inject(Router);
  route = inject(ActivatedRoute);

  expense: Expense | undefined;
  categories = signal<Category[]>([]);
  plans = signal<Plan[]>([]);
  errorMessage = signal('');

  expenseModel = signal<ExpenseFormModel>({
    categoryId: '0',
    amount: 0,
    expenseDate: '',
    description: '',
  });

  expenseForm = form(
    this.expenseModel,
    (schemaPath) => {
      required(schemaPath.categoryId, { message: 'Category is required' });
      min(schemaPath.amount, 0.01, { message: 'Amount must be greater than 0' });
      required(schemaPath.expenseDate, { message: 'Date is required' });
    },
    {
      submission: {
        action: async (form) => {
          this.errorMessage.set('');
          const value = form().value();
          await this.submit(
            {
              categoryId: +value.categoryId,
              amount: value.amount,
              expenseDate: value.expenseDate,
              description: value.description,
            },
            false,
          );
        },
      },
    },
  );

  private async submit(request: { categoryId: number; amount: number; expenseDate: string; description: string }, confirmOverBudget: boolean,): Promise<void> {
    const body = { ...request, confirmOverBudget };
    try {
      if (this.expense) {
        await firstValueFrom(this.service.update(this.expense.id, body));
      } else {
        await firstValueFrom(this.service.save(body));
      }
      this.router.navigate(['/expenses']);
    } catch (err) {
      const e = err as HttpErrorResponse;
      if (e?.status === 409 && e.error?.error === 'over_budget') {
        if (confirm(e.error.message ?? 'This expense exceeds your saving plan. Add it anyway?')) {
          await this.submit(request, true);
        }
        return;
      }
      this.errorMessage.set(e?.error?.error ?? 'Could not save the expense. Please try again.');
    }
  }

  ngOnInit(): void {
    this.categoryService.getCategories().subscribe((categories) => this.categories.set(categories));
    this.planService.getPlans().subscribe((plans) => this.plans.set(plans));


    this.route.paramMap
      .pipe(
        map((params) => params.get('id')),
        mergeMap((id) => {
          if (id) {
            return this.service.getExpenseById(+id);
          } else {
            return of(undefined);
          }
        }),
      )
      .subscribe((expense) => {
        if (expense) {
          this.expenseModel.set({
            categoryId: String(expense.categoryId),
            amount: expense.amount,
            expenseDate: expense.expenseDate,
            description: expense.description ?? '',
          });
          this.expense = expense;
        }
      });
  }
}

interface ExpenseFormModel {
  categoryId: string;
  amount: number;
  expenseDate: string;
  description: string;
}
