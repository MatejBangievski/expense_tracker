import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AvailablePeriod,
  ComparePeriodsRequest,
  PeriodComparison,
  PeriodType,
} from '../models/period-comparison';

@Service()
export class ComparisonService {
  http = inject(HttpClient);

  availablePeriods(periodType: PeriodType): Observable<AvailablePeriod[]> {
    return this.http.get<AvailablePeriod[]>(`/api/period-summaries`, {
      params: new HttpParams().set('periodType', periodType),
    });
  }

  compare(request: ComparePeriodsRequest, useAi: boolean): Observable<PeriodComparison> {
    return this.http.post<PeriodComparison>(`/api/period-comparisons`, request, {
      params: new HttpParams().set('useAi', useAi),
    });
  }
}
