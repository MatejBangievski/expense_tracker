import { HttpClient } from '@angular/common/http';
import { Injectable, inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { DailyPlan, DailyPlanRequest } from '../models/daily-plan';

@Service()
export class DailyPlanService {
  http = inject(HttpClient);

  getDailyPlans(planId: number): Observable<DailyPlan[]> {
    return this.http.get<DailyPlan[]>(`/api/plans/${planId}/daily`);
  }

  save(planId: number, request: DailyPlanRequest) {
    return this.http.post<DailyPlan>(`/api/plans/${planId}/daily`, request);
  }

  update(planId: number, dailyPlanId: number, request: DailyPlanRequest) {
    return this.http.put<DailyPlan>(`/api/plans/${planId}/daily/${dailyPlanId}`, request);
  }

  deleteDailyPlan(planId: number, dailyPlanId: number) {
    return this.http.delete(`/api/plans/${planId}/daily/${dailyPlanId}`);
  }
}
