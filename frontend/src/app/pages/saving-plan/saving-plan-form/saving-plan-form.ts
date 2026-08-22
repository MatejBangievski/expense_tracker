import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { form, FormField, FormRoot, min } from '@angular/forms/signals';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom, map, mergeMap, of } from 'rxjs';
import { CategoryService } from '../../../services/category.service';
import { SavingPlanService } from '../../../services/saving-plan.service';
import { Category } from '../../../models/category';
import { ManualSavingPlanRequest, SavingPlan } from '../../../models/saving-plan';

interface LimitRow {
  categoryName: string;
  suggestedLimit: number;
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
            categoryLimits: this.rows(),
          };

          try {
            await firstValueFrom(this.savingPlanService.createManual(request));
            await this.router.navigate(['/saving-plan']);
          } catch {
            this.errorMessage.set('Could not save the plan. Please try again.');
          }
        },
      },
    },
  );

  ngOnInit(): void {
    this.categoryService
      .getCategories()
      .subscribe((categories) => this.categories.set(categories.filter((c) => c.parentCategoryId === null)));

    this.route.data
      .pipe(
        map((data) => data['edit'] === true),
        mergeMap((editing) => (editing ? this.savingPlanService.getCurrentPlan() : of(undefined))),
      )
      .subscribe((plan) => {
        if (plan) {
          this.budgetModel.set({ budgetLimit: plan.totalBudgetLimit });
          this.rows.set(
            plan.categoryLimits.map((cl) => ({ categoryName: cl.categoryName, suggestedLimit: cl.monthlyLimit })),
          );
          this.plan = plan;
        }
      });
  }

  addSelectedCategory(): void {
    const name = this.selectedCategory();
    if (!name || this.rows().some((r) => r.categoryName === name)) {
      return;
    }
    this.rows.update((rows) => [...rows, { categoryName: name, suggestedLimit: 0 }]);
    this.selectedCategory.set('');
  }

  removeRow(index: number): void {
    this.rows.update((rows) => rows.filter((_, i) => i !== index));
  }

  changeLimit(index: number, delta: number): void {
    this.rows.update((rows) => {
      const total = rows.reduce((sum, r) => sum + r.suggestedLimit, 0);
      const remaining = this.budgetModel().budgetLimit - total;

      const add = delta > 0 ? Math.max(0, Math.min(delta, remaining)) : delta;
      return rows.map((row, i) =>
        i === index ? { ...row, suggestedLimit: Math.max(0, row.suggestedLimit + add) } : row,
      );
    });
  }

  setLimit(index: number, value: number): void {
    const budget = this.budgetModel().budgetLimit;
    this.rows.update((rows) => {
      const sumOthers = rows.reduce((sum, r, i) => (i === index ? sum : sum + r.suggestedLimit), 0);
      const maxForRow = Math.max(0, budget - sumOthers);
      const raw = Number.isFinite(value) ? Math.max(0, value) : 0;
      const safe = Math.min(raw, maxForRow);
      return rows.map((row, i) => (i === index ? { ...row, suggestedLimit: safe } : row));
    });
  }
}
