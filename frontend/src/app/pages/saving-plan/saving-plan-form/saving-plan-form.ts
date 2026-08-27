import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { form, FormField, FormRoot, min } from '@angular/forms/signals';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom, forkJoin } from 'rxjs';
import { CategoryService } from '../../../services/category.service';
import { SavingPlanService } from '../../../services/saving-plan.service';
import { Category } from '../../../models/category';
import { ManualSavingPlanRequest, SavingPlan } from '../../../models/saving-plan';

interface LimitRow {
  categoryName: string;
  suggestedLimit: number;
  minLimit: number;
}

@Component({
  selector: 'app-saving-plan-form',
  imports: [FormField, FormRoot, RouterLink, CurrencyPipe],
  templateUrl: './saving-plan-form.html',
  styleUrl: './saving-plan-form.css',
})
export class SavingPlanForm implements OnInit {
  categoryService = inject(CategoryService);
  savingPlanService = inject(SavingPlanService);
  router = inject(Router);
  route = inject(ActivatedRoute);

  readonly step = 10;
  readonly editing = this.route.snapshot.data['edit'] === true;

  plan: SavingPlan | undefined;
  categories = signal<Category[]>([]);
  rows = signal<LimitRow[]>([]);
  selectedCategory = signal('');
  errorMessage = signal('');

  availableCategories = computed(() =>
    this.categories().filter((c) => !this.rows().some((r) => r.categoryName === c.name)),
  );

  totalAllocated = computed(() => this.rows().reduce((sum, r) => sum + r.suggestedLimit, 0));
  remaining = computed(() => this.budgetModel().budgetLimit - this.totalAllocated());
  totalSpent = computed(() => this.rows().reduce((sum, r) => sum + r.minLimit, 0));

  budgetModel = signal<{ budgetLimit: number }>({ budgetLimit: 0 });

  budgetForm = form(
    this.budgetModel,
    (schemaPath) => {
      min(schemaPath.budgetLimit, 0, { message: 'Budget cannot be negative' });
    },
    {
      submission: {
        action: async (form) => {
          this.errorMessage.set('');
          const request: ManualSavingPlanRequest = {
            budgetLimit: form().value().budgetLimit,
            categoryLimits: this.rows().map((r) => ({ categoryName: r.categoryName, suggestedLimit: r.suggestedLimit })),
          };
          try {
            await firstValueFrom(this.savingPlanService.createManual(request));
            await this.router.navigate(['/saving-plan']);
          } catch (err) {
            this.errorMessage.set(
              (err as HttpErrorResponse)?.error?.error ?? 'Could not save the plan. Please try again.',
            );
          }
        },
      },
    },
  );

  ngOnInit(): void {
    this.categoryService
      .getCategories()
      .subscribe((cats) => this.categories.set(cats.filter((c) => c.parentCategoryId === null)));

    if (this.editing) {
      forkJoin({
        plan: this.savingPlanService.getCurrentPlan(),
        spending: this.savingPlanService.getCurrentSpending(),
      }).subscribe(({ plan, spending }) => {
        if (!plan) {
          return;
        }
        this.plan = plan;
        const liveSpentByName = new Map(spending.categories.map((c) => [c.categoryName, c.totalAmount]));
        const rows: LimitRow[] = plan.categoryLimits.map((cl) => {
          const liveSpent = liveSpentByName.get(cl.categoryName) ?? 0;
          liveSpentByName.delete(cl.categoryName);
          return {
            categoryName: cl.categoryName,
            suggestedLimit: Math.max(cl.monthlyLimit, liveSpent),
            minLimit: liveSpent,
          };
        });
        for (const [categoryName, spent] of liveSpentByName) {
          if (spent > 0) {
            rows.push({ categoryName, suggestedLimit: spent, minLimit: spent });
          }
        }
        this.rows.set(rows);
        this.budgetModel.set({ budgetLimit: Math.max(plan.totalBudgetLimit, spending.totalSpent) });
      });
    } else {
      this.savingPlanService.getCurrentSpending().subscribe((spending) => {
        this.rows.set(
          spending.categories
            .filter((c) => c.totalAmount > 0)
            .map((c) => ({ categoryName: c.categoryName, suggestedLimit: c.totalAmount, minLimit: c.totalAmount })),
        );
        this.budgetModel.set({ budgetLimit: spending.totalSpent });
      });
    }
  }

  addSelectedCategory(): void {
    const name = this.selectedCategory();
    if (!name || this.rows().some((r) => r.categoryName === name)) {
      return;
    }
    this.rows.update((rows) => [...rows, { categoryName: name, suggestedLimit: 0, minLimit: 0 }]);
    this.selectedCategory.set('');
  }

  removeRow(index: number): void {
    this.rows.update((rows) => {
      if ((rows[index]?.minLimit ?? 0) > 0) {
        return rows;
      }
      return rows.filter((_, i) => i !== index);
    });
  }

  changeLimit(index: number, delta: number): void {
    this.rows.update((rows) => {
      const total = rows.reduce((sum, r) => sum + r.suggestedLimit, 0);
      const remaining = this.budgetModel().budgetLimit - total;

      const add = delta > 0 ? Math.max(0, Math.min(delta, remaining)) : delta;
      return rows.map((row, i) =>
        i === index ? { ...row, suggestedLimit: Math.max(row.minLimit, row.suggestedLimit + add) } : row,
      );
    });
  }

  setLimit(index: number, value: number): void {
    const budget = this.budgetModel().budgetLimit;
    this.rows.update((rows) => {
      const row = rows[index];
      const sumOthers = rows.reduce((sum, r, i) => (i === index ? sum : sum + r.suggestedLimit), 0);
      const maxForRow = Math.max(row.minLimit, budget - sumOthers);
      const raw = Number.isFinite(value) ? Math.max(row.minLimit, value) : row.minLimit;
      const safe = Math.min(raw, maxForRow);
      return rows.map((r, i) => (i === index ? { ...r, suggestedLimit: safe } : r));
    });
  }
}
