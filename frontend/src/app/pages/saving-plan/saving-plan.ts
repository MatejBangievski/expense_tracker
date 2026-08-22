import { Component, OnInit, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ReplaySubject, mergeMap } from 'rxjs';
import { RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { SavingPlanService } from '../../services/saving-plan.service';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-saving-plan',
  imports: [RouterLink, CurrencyPipe],
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

  ngOnInit(): void {
    this.reload$.next();
    this.userService.getApiKey().subscribe((key) => this.hasApiKey.set(key !== null));
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
