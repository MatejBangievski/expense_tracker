import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { PlanItem, PlanItemRequest } from '../models/plan-item';

@Service()
export class PlanItemService {
  http = inject(HttpClient);

  getPlanItems(planId: number): Observable<PlanItem[]> {
    return this.http.get<PlanItem[]>(`/api/plans/${planId}/items`);
  }

  save(planId: number, request: PlanItemRequest) {
    return this.http.post<PlanItem>(`/api/plans/${planId}/items`, request);
  }

  update(planId: number, itemId: number, request: PlanItemRequest) {
    return this.http.put<PlanItem>(`/api/plans/${planId}/items/${itemId}`, request);
  }

  deleteItem(planId: number, itemId: number) {
    return this.http.delete(`/api/plans/${planId}/items/${itemId}`);
  }
}
