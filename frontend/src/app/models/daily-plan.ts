export interface DailyPlan {
  id: number;
  planId: number;
  date: string;
  allocatedAmount: number;
  actualSpent: number;
}

export interface DailyPlanRequest {
  date: string;
  allocatedAmount: number;
  confirmOverBudget?: boolean;
}
