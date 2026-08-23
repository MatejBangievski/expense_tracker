import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Result } from '../models/result';
import { Plan, PlanRequest } from '../models/plan';

@Service()
export class PlanService {
  http = inject(HttpClient);

  getPlans(): Observable<Plan[]> {
    return this.http.get<Plan[]>(`/api/plans`);
  }

  getPlansResult(): Observable<Result<Plan[]>> {
    return this.http.get<Plan[]>(`/api/plans`).pipe(
      map((result) => ({ data: result, loading: false })),
    );
  }

  save(request: PlanRequest) {
    return this.http.post<Plan>(`/api/plans`, request);
  }

  update(id: number, request: PlanRequest) {
    return this.http.put<Plan>(`/api/plans/${id}`, request);
  }

  deletePlan(id: number) {
    return this.http.delete(`/api/plans/${id}`);
  }
}
