import { Component, inject, OnInit, signal } from '@angular/core';
import { form, FormField, FormRoot, required, min } from '@angular/forms/signals';
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
  imports: [FormField, FormRoot],
  templateUrl: './plan-form.html',
  styleUrl: './plan-form.css',
})
export class PlanForm implements OnInit {
  service = inject(PlanService);
  router = inject(Router);
  route = inject(ActivatedRoute);

  plan: Plan | undefined;

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

          let result;
          if (this.plan) {
            result = await firstValueFrom(this.service.update(this.plan.id, value));
          } else {
            result = await firstValueFrom(this.service.save(value));
          }
          console.log('result', result);
          this.router.navigate(['/plans']);
          return;
        },
      },
    },
  );

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        map((params) => params.get('id')),
        mergeMap((id) => {
          if (id) {
            return this.service.getPlans().pipe(map((all) => all.find((p) => p.id === +id)));
          } else {
            return of(undefined);
          }
        }),
      )
      .subscribe((plan) => {
        if (plan) {
          this.planModel.set({
            name: plan.name,
            startDate: plan.startDate,
            endDate: plan.endDate,
            totalBudget: plan.totalBudget,
          });
          this.plan = plan;
        }
      });
  }
}
