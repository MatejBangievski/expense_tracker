import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { form, FormField, FormRoot, required, min } from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';
import { PlanItemService } from '../../../services/plan-item.service';
import { DailyPlanService } from '../../../services/daily-plan.service';
import { CategoryService } from '../../../services/category.service';
import { PlanService } from '../../../services/plan.service';
import { PlanItem } from '../../../models/plan-item';
import { DailyPlan } from '../../../models/daily-plan';
import { Category } from '../../../models/category';
import { Plan } from '../../../models/plan';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { CHART_DANGER, CHART_TRACK } from '../../../shared/chart-colors';

interface NewItemForm {
  categoryId: string;
  description: string;
  plannedDate: string;
  plannedAmount: number;
}

interface NewDailyPlanForm {
  date: string;
  allocatedAmount: number;
}

@Component({
  selector: 'app-plan-detail',
  imports: [FormField, FormRoot, RouterLink, CurrencyPipe, DatePipe, BaseChartDirective],
  templateUrl: './plan-detail.html',
  styleUrl: './plan-detail.css',
})
export class PlanDetail implements OnInit {
  route = inject(ActivatedRoute);
  itemService = inject(PlanItemService);
  dailyPlanService = inject(DailyPlanService);
  categoryService = inject(CategoryService);
  planService = inject(PlanService);

  planId = 0;

  plan = signal<Plan | undefined>(undefined);
  items = signal<PlanItem[]>([]);
  dailyPlans = signal<DailyPlan[]>([]);
  categories = signal<Category[]>([]);

  budget = computed(() => this.plan()?.totalBudget ?? 0);
  plannedTotal = computed(() => this.items().reduce((sum, item) => sum + item.plannedAmount, 0));
  remaining = computed(() => Math.max(this.budget() - this.plannedTotal(), 0));
  overBudget = computed(() => this.plannedTotal() > this.budget());
  overBy = computed(() => Math.max(this.plannedTotal() - this.budget(), 0));
  budgetPercent = computed(() => {
    const budget = this.budget();
    return budget > 0 ? Math.round(Math.min(this.plannedTotal() / budget, 1) * 100) : 0;
  });

  budgetDonutData = computed<ChartData<'doughnut'>>(() => ({
    labels: ['Spent', 'Remaining'],
    datasets: [
      {
        data: [this.plannedTotal(), this.remaining()],
        backgroundColor: [CHART_DANGER, CHART_TRACK],
        borderWidth: 0,
      },
    ],
  }));

  budgetDonutOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '72%',
    plugins: {
      legend: { display: false },
      tooltip: { enabled: false },
    },
  };

  itemError = signal('');
  dailyPlanError = signal('');

  newItemModel = signal<NewItemForm>({
    categoryId: '0',
    description: '',
    plannedDate: '',
    plannedAmount: 0
  });

  newItemForm = form(
    this.newItemModel,
    (schemaPath) => {
      required(schemaPath.description, {
        message: 'Description is required'
      });

      required(schemaPath.plannedDate, {
        message: 'Date is required'
      });

      min(schemaPath.plannedAmount, 0.01, {
        message: 'Amount must be greater than 0'
      });
    },
    {
      submission: {
        action: async (form) => {
          await this.submitItem(form().value(), false);
          return;
        }
      }
    }
  );

  newDailyPlanModel = signal<NewDailyPlanForm>({
    date: '',
    allocatedAmount: 0
  });

  newDailyPlanForm = form(
    this.newDailyPlanModel,
    (schemaPath) => {
      required(schemaPath.date, {
        message: 'Date is required'
      });

      min(schemaPath.allocatedAmount, 0.01, {
        message: 'Amount must be greater than 0'
      });
    },
    {
      submission: {
        action: async (form) => {
          await this.submitDailyPlan(form().value(), false);
          return;
        }
      }
    }
  );

  ngOnInit(): void {
    this.planId = +this.route.snapshot.paramMap.get('id')!;

    this.loadItems();
    this.loadDailyPlans();

    this.planService
      .getPlans()
      .subscribe((plans) => this.plan.set(plans.find((plan) => plan.id === this.planId)));

    this.categoryService
      .getCategories()
      .subscribe((categories) => this.categories.set(categories));
  }

  loadItems(): void {
    this.itemService
      .getPlanItems(this.planId)
      .subscribe((items) => this.items.set(items));
  }

  loadDailyPlans(): void {
    this.dailyPlanService
      .getDailyPlans(this.planId)
      .subscribe((dailyPlans) => this.dailyPlans.set(dailyPlans));
  }

  onDeleteItem(itemId: number): void {
    this.itemService
      .deleteItem(this.planId, itemId)
      .subscribe(() => {
        this.loadItems();
        this.loadDailyPlans();
      });
  }

  onDeleteDailyPlan(dailyPlanId: number): void {
    this.dailyPlanService
      .deleteDailyPlan(this.planId, dailyPlanId)
      .subscribe(() => {
        this.loadDailyPlans();
      });
  }

  async submitItem(value: NewItemForm, confirmOverBudget: boolean) {
    this.itemError.set('');

    const request = {
      categoryId: value.categoryId !== '0' ? +value.categoryId : null,
      description: value.description,
      plannedDate: value.plannedDate,
      plannedAmount: value.plannedAmount,
      confirmOverBudget
    };

    try {
      await firstValueFrom(
        this.itemService.save(this.planId, request)
      );

      this.loadItems();
      this.loadDailyPlans();

      this.newItemForm().reset({
        categoryId: '0',
        description: '',
        plannedDate: '',
        plannedAmount: 0
      });

    } catch (error: any) {
      if (error.error?.error === 'over_budget') {
        const remaining = error.error.remainingBudget;

        const confirmed = confirm(
          `This exceeds your remaining plan budget of ${remaining}. Add it anyway?`
        );

        if (confirmed) {
          await this.submitItem(value, true);
        }
      } else if (error.error?.error) {
        this.itemError.set(error.error.error);
      } else {
        this.itemError.set('Could not add item');
      }
    }
  }

  async submitDailyPlan(
    value: NewDailyPlanForm,
    confirmOverBudget: boolean
  ): Promise<void> {
    this.dailyPlanError.set('');

    const request = {
      date: value.date,
      allocatedAmount: value.allocatedAmount,
      confirmOverBudget
    };

    try {
      await firstValueFrom(
        this.dailyPlanService.save(this.planId, request)
      );

      this.loadDailyPlans();

      this.newDailyPlanForm().reset({
        date: '',
        allocatedAmount: 0
      });

    } catch (error: any) {

      if (error.error?.error === 'over_budget') {
        const remaining = error.error.remainingBudget;

        const confirmed = confirm(
          `This exceeds your remaining plan budget of ${remaining}. Allocate it anyway?`
        );

        if (confirmed) {
          await this.submitDailyPlan(value, true);
        }

      } else {
        this.dailyPlanError.set(
          'Could not add daily allocation'
        );
      }
    }
  }
}
