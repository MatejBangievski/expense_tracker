import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { Result } from '../models/result';
import {
  CurrentSpending,
  GenerateAiRequest,
  ManualSavingPlanRequest,
  SavingPlan,
} from '../models/saving-plan';

@Service()
export class SavingPlanService {
  http = inject(HttpClient);

  getCurrentPlan(): Observable<SavingPlan | null> {
    return this.http.get<SavingPlan>(`/api/monthly-saving-plan`).pipe(catchError(() => of(null)));
  }

  getCurrentPlanResult(): Observable<Result<SavingPlan | null>> {
    return this.getCurrentPlan().pipe(map((data) => ({ data, loading: false })));
  }

  getCurrentSpending(): Observable<CurrentSpending> {
    return this.http.get<CurrentSpending>(`/api/period-summaries/current`);
  }

  createManual(request: ManualSavingPlanRequest): Observable<SavingPlan> {
    return this.http.post<SavingPlan>(`/api/monthly-saving-plan`, request);
  }

  generateAi(request: GenerateAiRequest): Observable<SavingPlan> {
    return this.http.post<SavingPlan>(`/api/monthly-saving-plan/ai`, request);
  }
}
