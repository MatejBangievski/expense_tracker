export const CHART_COLORS = [
  '#3b82f6',
  '#34c77b',
  '#f5c451',
  '#ef5b5b',
  '#a78bfa',
  '#f97316',
  '#14b8a6',
  '#ec4899',
];
export const CHART_TRACK = '#e5e7eb';
export const CHART_DANGER = '#ef5b5b';

export const chartColor = (i: number): string => CHART_COLORS[i % CHART_COLORS.length];
