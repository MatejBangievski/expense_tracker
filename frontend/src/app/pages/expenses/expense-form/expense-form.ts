import { Component, inject, OnInit, signal } from '@angular/core';
import { form, FormField, FormRoot, min, required } from '@angular/forms/signals';
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
          const value = form().value();
          const request = {
            categoryId: +value.categoryId,
            amount: value.amount,
            expenseDate: value.expenseDate,
            description: value.description,
          };

          let result;
          if (this.expense) {
            result = await firstValueFrom(this.service.update(this.expense.id, request));
          } else {
            result = await firstValueFrom(this.service.save(request));
          }
          console.log('result', result);
          this.router.navigate(['/expenses']);
          return;
        },
      },
    },
  );

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
