import { Component, inject, OnInit, signal } from '@angular/core';
import { form, FormField, FormRoot, required, min } from '@angular/forms/signals';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PlanService } from '../../../services/plan.service';
import { firstValueFrom, map, mergeMap, of } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { Plan } from '../../../models/plan';

interface PlanFormModel {
  name: string;
  startDate: string;
  endDate: string;
  totalBudget: number;
}

@Component({
  selector: 'app-plan-form',
  imports: [FormField, FormRoot, RouterLink, CurrencyPipe, DatePipe],
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
