export interface CategoryLimit {
  categoryId: number;
  categoryName: string;
  monthlyLimit: number;
  actualSpent: number | null;
  reason: string | null;
}

export interface SavingPlan {
  id: number;
  savingMonth: string;
  totalBudgetLimit: number;
  totalIncome: number | null;
  totalSpent: number;
  totalSaved: number;
  recommendationMessage: string | null;
  categoryLimits: CategoryLimit[];
}

export interface ManualSavingPlanRequest {
  budgetLimit: number;
  categoryLimits: { categoryName: string; suggestedLimit: number }[];
}

export interface GenerateAiRequest {
  budgetLimit: number;
}
