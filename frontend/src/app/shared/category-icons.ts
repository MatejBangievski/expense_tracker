import { CHART_COLORS } from './chart-colors';

const CATEGORY_ICON_MAP: Record<string, string> = {
  food: 'food',
  groceries: 'food',
  'dining out': 'food',
  dining: 'food',
  restaurant: 'food',
  transportation: 'transport',
  transport: 'transport',
  fuel: 'transport',
  'public transit': 'transport',
  entertainment: 'entertainment',
  'streaming subscriptions': 'entertainment',
  streaming: 'entertainment',
  'video games': 'entertainment',
  games: 'entertainment',
  utilities: 'utilities',
  electricity: 'utilities',
  internet: 'utilities',
  bills: 'utilities',
  shopping: 'shopping',
  clothing: 'shopping',
  health: 'health',
  healthcare: 'health',
  medical: 'health',
  travel: 'travel',
  vacation: 'travel',
  rent: 'home',
  housing: 'home',
  home: 'home',
  salary: 'income',
  income: 'income',
};

export const categoryIcon = (name?: string | null): string =>
  name ? CATEGORY_ICON_MAP[name.trim().toLowerCase()] ?? 'generic' : 'generic';

export const categoryColor = (name: string): string => {
  let hash = 0;
  for (const ch of name) {
    hash = (hash * 31 + ch.charCodeAt(0)) >>> 0;
  }
  return CHART_COLORS[hash % CHART_COLORS.length];
};
