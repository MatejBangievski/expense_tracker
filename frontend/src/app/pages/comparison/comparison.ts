import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ComparisonService } from '../../services/comparison.service';
import { UserService } from '../../services/user.service';
import { AvailablePeriod, PeriodComparison, PeriodType, periodLabel } from '../../models/period-comparison';
import { Spinner } from '../../shared/spinner/spinner';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';
import { CHART_COLORS, CHART_TRACK } from '../../shared/chart-colors';

@Component({
  selector: 'app-comparison',
  imports: [CurrencyPipe, DecimalPipe, RouterLink, Spinner, BaseChartDirective],
  templateUrl: './comparison.html',
})
export class Comparison implements OnInit {
  comparisonService = inject(ComparisonService);
  userService = inject(UserService);
  route = inject(ActivatedRoute);

  readonly periodTypes: PeriodType[] = ['WEEK', 'MONTH', 'YEAR'];

  periodType = signal<PeriodType>('MONTH');
  periods = signal<AvailablePeriod[]>([]);
  periodA = signal('');
  periodB = signal('');
  useAi = signal(false);
  hasApiKey = signal(false);

  result = signal<PeriodComparison | null>(null);
  loading = signal(false);
  error = signal('');

  totalDelta = computed(() => {
    const r = this.result();
    return r ? r.currentTotalSpent - r.previousTotalSpent : 0;
  });

  optionsA = computed(() => this.periods().filter((p) => p.periodStart !== this.periodB()));
  optionsB = computed(() => this.periods().filter((p) => p.periodStart !== this.periodA()));

  categoryChartData = computed<ChartData<'bar'>>(() => {
    const r = this.result();
    if (!r) {
      return { labels: [], datasets: [] };
    }
    return {
      labels: r.categories.map((c) => c.categoryName),
      datasets: [
        {
          label: this.label(r.previousPeriodStart, r.periodType),
          data: r.categories.map((c) => c.previousAmount),
          backgroundColor: CHART_TRACK,
          borderWidth: 0,
        },
        {
          label: this.label(r.currentPeriodStart, r.periodType),
          data: r.categories.map((c) => c.currentAmount),
          backgroundColor: CHART_COLORS[0],
          borderWidth: 0,
        },
      ],
    };
  });

  categoryChartOptions: ChartOptions<'bar'> = {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: { ticks: { callback: (value) => '$' + value } },
    },
    plugins: {
      legend: { position: 'top' },
      tooltip: {
        callbacks: {
          label: (ctx) => `${ctx.dataset.label}: $${Number(ctx.parsed.x).toFixed(2)}`,
        },
      },
    },
  };

  chartHeight = computed(() => Math.max(200, (this.result()?.categories.length ?? 0) * 44 + 60));

  ngOnInit(): void {
    this.userService.getApiKey().subscribe((key) => this.hasApiKey.set(key !== null));

    const params = this.route.snapshot.queryParamMap;
    const type = params.get('type') as PeriodType | null;
    if (type && this.periodTypes.includes(type)) {
      this.periodType.set(type);
    }
    this.loadPeriods(params.get('current'), params.get('previous'), params.get('run') === '1');
  }

  onTypeChange(type: string): void {
    this.periodType.set(type as PeriodType);
    this.result.set(null);
    this.error.set('');
    this.loadPeriods();
  }

  private loadPeriods(preferredCurrent?: string | null, preferredPrevious?: string | null, autoRun = false): void {
    this.comparisonService.availablePeriods(this.periodType()).subscribe((periods) => {
      this.periods.set(periods);
      this.periodA.set(preferredCurrent || periods[0]?.periodStart || '');
      this.periodB.set(preferredPrevious || periods[1]?.periodStart || '');
      if (autoRun && this.periodA() && this.periodB()) {
        this.compare();
      }
    });
  }

  compare(): void {
    const current = this.periodA();
    const previous = this.periodB();
    if (!current || !previous) {
      this.error.set('Pick two periods to compare.');
      return;
    }
    if (current === previous) {
      this.error.set('Pick two different periods.');
      return;
    }
    this.error.set('');
    this.loading.set(true);
    this.comparisonService
      .compare(
        {
          currentPeriodType: this.periodType(),
          currentDate: current,
          previousPeriodType: this.periodType(),
          previousDate: previous,
        },
        this.useAi(),
      )
      .subscribe({
        next: (comparison) => {
          this.loading.set(false);
          this.result.set(comparison);
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          this.error.set(err.error?.error ?? 'Could not compare the selected periods.');
        },
      });
  }

  percentChange(current: number, previous: number): number | null {
    if (current <= 0 || previous <= 0) {
      return null;
    }
    return ((current - previous) / previous) * 100;
  }

  label(iso: string, type: PeriodType): string {
    return periodLabel(iso, type);
  }
}
