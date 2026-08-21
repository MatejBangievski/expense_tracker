export interface Expense {
  id: number;
  categoryId: number;
  categoryName: string;
  amount: number;
  expenseDate: string;
  description: string | null;
}

export interface ExpenseFilter {
  categoryName?: string;
  periodStart?: string;
  periodEnd?: string;
  search?: string;
}
