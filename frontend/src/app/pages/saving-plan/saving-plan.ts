import { Component, OnInit, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ReplaySubject, mergeMap, forkJoin, map } from 'rxjs';
import { RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { SavingPlanService } from '../../services/saving-plan.service';
import { UserService } from '../../services/user.service';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { Spinner } from '../../shared/spinner/spinner';


@Component({
  selector: 'app-saving-plan',
  imports: [RouterLink, CurrencyPipe, MatProgressSpinner, Spinner],
  templateUrl: './saving-plan.html',
  styleUrl: './saving-plan.css',
})
export class SavingPlan implements OnInit {
  savingPlanService = inject(SavingPlanService);
  userService = inject(UserService);
  // expenseService = inject(ExpenseService);

  reload$ = new ReplaySubject<void>();

  plan = toSignal(
    this.reload$.pipe(mergeMap(() => this.savingPlanService.getCurrentPlanResult())),
    { initialValue: { data: undefined, loading: true } },
  );

  // plan = toSignal(
  //   this.reload$.pipe(
  //     mergeMap(() =>
  //       forkJoin({
  //         plan: this.savingPlanService.getCurrentPlan(),
  //         expenses: this.expenseService.getExpenses(),
  //       }).pipe(
  //         map(({ plan, expenses }) => {
  //           if (!plan) {
  //             return {
  //               data: null,
  //               loading: false,
  //             };
  //           }
  //
  //           const totalSpent = expenses.reduce(
  //             (sum, expense) => sum + expense.amount,
  //             0,
  //           );
  //
  //           const categoryLimits = plan.categoryLimits.map((category) => {
  //             const actualSpent = expenses
  //               .filter(
  //                 (expense) => expense.categoryId === category.categoryId,
  //               )
  //               .reduce((sum, expense) => sum + expense.amount, 0);
  //
  //             return {
  //               ...category,
  //               actualSpent,
  //             };
  //           });
  //
  //           return {
  //             data: {
  //               ...plan,
  //               totalSpent,
  //               totalSaved:
  //                 plan.totalIncome != null
  //                   ? plan.totalIncome - totalSpent
  //                   : plan.totalSaved,
  //               categoryLimits,
  //             },
  //             loading: false,
  //           };
  //         }),
  //       ),
  //     ),
  //   ),
  //   {
  //     initialValue: {
  //       data: null,
  //       loading: true,
  //     },
  //   },
  // );

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
