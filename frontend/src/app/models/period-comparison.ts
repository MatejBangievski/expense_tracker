import { formatDate } from '@angular/common';

export type PeriodType = 'WEEK' | 'MONTH' | 'YEAR';

export function periodLabel(iso: string, type: PeriodType): string {
  switch (type) {
    case 'YEAR':
      return formatDate(iso, 'yyyy', 'en-US');
    case 'MONTH':
      return formatDate(iso, 'MMMM yyyy', 'en-US');
    case 'WEEK':
      return `Week of ${formatDate(iso, 'MMM d, yyyy', 'en-US')}`;
  }
}

export interface AvailablePeriod {
  periodStart: string;
  totalSpent: number;
}

export interface ComparePeriodsRequest {
  currentPeriodType: PeriodType;
  currentDate: string;
  previousPeriodType: PeriodType;
  previousDate: string;
}

export interface CategoryComparison {
  categoryName: string;
  currentAmount: number;
  previousAmount: number;
}

export interface PeriodComparison {
  id: number;
  periodType: PeriodType;
  currentPeriodStart: string;
  previousPeriodStart: string;
  currentTotalSpent: number;
  previousTotalSpent: number;
  comparisonMessage: string | null;
  categories: CategoryComparison[];
}
