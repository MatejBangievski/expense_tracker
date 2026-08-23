import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { ReplaySubject, mergeMap } from 'rxjs';
import { PlanService } from '../../services/plan.service';
import { CurrencyPipe } from '@angular/common';

@Component({
  selector: 'app-plans',
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './plans.html',
  styleUrl: './plans.css',
})
export class Plans implements OnInit {
  service = inject(PlanService);
  reload$ = new ReplaySubject<void>();

  plans = toSignal(
    this.reload$.pipe(mergeMap(() => this.service.getPlansResult())),
    { initialValue: { data: undefined, loading: true } },
  );

  ngOnInit(): void {
    this.reload$.next();
  }

  onDelete(id: number) {
    this.service.deletePlan(id).subscribe(() => this.reload$.next());
  }
}
