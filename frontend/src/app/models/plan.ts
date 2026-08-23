export interface Plan {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  totalBudget: number;
  totalPlanned: number;
}
export interface PlanRequest {
  name: string;
  startDate: string;
  endDate: string;
  totalBudget: number;
}
