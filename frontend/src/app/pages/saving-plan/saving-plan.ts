import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ReplaySubject, mergeMap } from 'rxjs';
import { RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { SavingPlanService } from '../../services/saving-plan.service';
import { UserService } from '../../services/user.service';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { Spinner } from '../../shared/spinner/spinner';


@Component({
  selector: 'app-saving-plan',
  imports: [RouterLink, CurrencyPipe, MatProgressSpinner, Spinner, DatePipe],
  templateUrl: './saving-plan.html',
  styleUrl: './saving-plan.css',
})
export class SavingPlan implements OnInit {
  savingPlanService = inject(SavingPlanService);
  userService = inject(UserService);

  reload$ = new ReplaySubject<void>();

  plan = toSignal(
    this.reload$.pipe(mergeMap(() => this.savingPlanService.getCurrentPlanResult())),
    { initialValue: { data: undefined, loading: true } },
  );

  hasApiKey = signal(false);
  showAi = signal(false);
  aiBudget = signal(0);
  aiError = signal('');
  aiLoading = signal(false);

  readonly percentageIncrease = 10;
  currentSpent = signal(0);
  minAiBudget = computed(() => Math.ceil(this.currentSpent() * (1 + this.percentageIncrease / 100) * 100) / 100,);

  ngOnInit(): void {
    this.reload$.next();
    this.userService.getApiKey().subscribe((key) => this.hasApiKey.set(key !== null));
    this.savingPlanService.getCurrentSpending().subscribe((spending) => {
      this.currentSpent.set(spending.totalSpent);
      this.aiBudget.set(this.minAiBudget());
    });
  }

  generateAi(): void {
    this.aiError.set('');
    this.aiLoading.set(true);
    this.savingPlanService.generateAi({ budgetLimit: this.aiBudget() }).subscribe({
      next: () => {
        this.aiLoading.set(false);
        this.showAi.set(false);
        this.reload$.next();
      },
      error: (err: HttpErrorResponse) => {
        this.aiLoading.set(false);
        if (err.status === 422) {
          this.aiError.set('No spending recorded yet this month — add expenses first.');
        } else if (err.status === 503) {
          this.aiError.set('The AI service is unavailable. Please try again.');
        } else {
          this.aiError.set('Could not generate a plan. Please try again.');
        }
      },
    });
  }
}
