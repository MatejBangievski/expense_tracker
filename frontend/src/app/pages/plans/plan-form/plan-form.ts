import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { form, FormField, FormRoot, required, min } from '@angular/forms/signals';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PlanService } from '../../../services/plan.service';
import { firstValueFrom, map, mergeMap, of } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { Plan } from '../../../models/plan';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import {
  MatDatepickerToggle,
  MatDateRangeInput,
  MatDateRangePicker,
  MatStartDate,
  MatEndDate,
  MatDatepickerInputEvent,
} from '@angular/material/datepicker';

interface PlanFormModel {
  name: string;
  startDate: string;
  endDate: string;
  totalBudget: number;
}

@Component({
  selector: 'app-plan-form',
  imports: [
    FormField,
    FormRoot,
    RouterLink,
    CurrencyPipe,
    DatePipe,
    MatFormField,
    MatLabel,
    MatDateRangeInput,
    MatDatepickerToggle,
    MatDateRangePicker,
    MatStartDate,
    MatEndDate,
  ],
  templateUrl: './plan-form.html',
  styleUrl: '../../form-page.css',
})
export class PlanForm implements OnInit {
  service = inject(PlanService);
  router = inject(Router);
  route = inject(ActivatedRoute);

  plan = signal<Plan | undefined>(undefined);

  planModel = signal<PlanFormModel>({
    name: '',
    startDate: '',
    endDate: '',
    totalBudget: 0,
  });

  startDateValue = computed(() => (this.planModel().startDate ? new Date(this.planModel().startDate) : null));
  endDateValue = computed(() => (this.planModel().endDate ? new Date(this.planModel().endDate) : null));

  onStartDateChange(event: MatDatepickerInputEvent<Date>) {
    const start = event.value;
    if (!start) {
      return;
    }
    this.planModel.update((model) => ({ ...model, startDate: this.formatDate(start) }));
  }

  onEndDateChange(event: MatDatepickerInputEvent<Date>) {
    const end = event.value;
    if (!end) {
      return;
    }
    this.planModel.update((model) => ({ ...model, endDate: this.formatDate(end) }));
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  planForm = form(
    this.planModel,
    (schemaPath) => {
      required(schemaPath.name, { message: 'Name is required' });
      required(schemaPath.startDate, { message: 'Start date is required' });
      required(schemaPath.endDate, { message: 'End date is required' });
      min(schemaPath.totalBudget, 0.01, { message: 'Budget must be greater than 0' });
    },
    {
      submission: {
        action: async (form) => {
          const value = form().value();

          const existing = this.plan();
          if (existing) {
            await firstValueFrom(this.service.update(existing.id, value));
          } else {
            await firstValueFrom(this.service.save(value));
          }
          this.router.navigate(['/plans']);
        },
      },
    },
  );

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        map((params) => params.get('id')),
        mergeMap((id) =>
          id ? this.service.getPlans().pipe(map((all) => all.find((p) => p.id === +id))) : of(undefined),
        ),
      )
      .subscribe((plan) => {
        if (plan) {
          this.planModel.set({
            name: plan.name,
            startDate: plan.startDate,
            endDate: plan.endDate,
            totalBudget: plan.totalBudget,
          });
          this.plan.set(plan);
        }
      });
  }
}
