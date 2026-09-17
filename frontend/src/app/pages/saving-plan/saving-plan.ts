import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ReplaySubject, mergeMap } from 'rxjs';
import { RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { SavingPlanService } from '../../services/saving-plan.service';
import { UserService } from '../../services/user.service';
import { Spinner } from '../../shared/spinner/spinner';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';
import { CHART_DANGER, CHART_TRACK, chartColor } from '../../shared/chart-colors';
import { Icon } from '../../shared/icon/icon';

@Component({
  selector: 'app-saving-plan',
  imports: [RouterLink, CurrencyPipe, Spinner, DatePipe, BaseChartDirective, Icon],
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

  private categoryLimits = computed(() => this.plan().data?.categoryLimits ?? []);

  breakdownLegend = computed(() =>
    this.categoryLimits().map((c, i) => ({
      name: c.categoryName,
      amount: c.actualSpent ?? 0,
      color: chartColor(i),
    })),
  );

  breakdownData = computed<ChartData<'bar'>>(() => ({
    labels: ['Spending'],
    datasets: this.categoryLimits().map((c, i) => ({
      label: c.categoryName,
      data: [c.actualSpent ?? 0],
      backgroundColor: chartColor(i),
      borderWidth: 0,
      stack: 'spending',
    })),
  }));

  breakdownOptions: ChartOptions<'bar'> = {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: { stacked: true, ticks: { callback: (value) => '$' + value } },
      y: { stacked: true },
    },
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: (ctx) => `${ctx.dataset.label}: $${Number(ctx.parsed.x).toFixed(2)}`,
        },
      },
    },
  };

  donuts = computed(() =>
    this.categoryLimits().map((c, i) => {
      const spent = c.actualSpent ?? 0;
      const limit = c.monthlyLimit;
      const over = spent > limit;
      const ratio = limit > 0 ? Math.min(spent / limit, 1) : spent > 0 ? 1 : 0;
      const data: ChartData<'doughnut'> = {
        labels: over ? ['Over budget'] : ['Spent', 'Remaining'],
        datasets: [
          {
            data: over ? [1] : [spent, Math.max(limit - spent, 0)],
            backgroundColor: over ? [CHART_DANGER] : [chartColor(i), CHART_TRACK],
            borderWidth: 0,
          },
        ],
      };
      return {
        name: c.categoryName,
        spent,
        limit,
        over,
        overBy: over ? spent - limit : 0,
        percent: Math.round(ratio * 100),
        data,
      };
    }),
  );

  donutOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '70%',
    plugins: {
      legend: { display: false },
      tooltip: { enabled: false },
    },
  };

  hasApiKey = signal(false);
  showAi = signal(false);
  aiBudget = signal(0);
  aiError = signal('');
  aiLoading = signal(false);

  readonly percentageIncrease = 10;
  currentSpent = signal(0);
  minAiBudget = computed(
    () => Math.ceil(this.currentSpent() * (1 + this.percentageIncrease / 100) * 100) / 100,
  );

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
