export interface PlanItem {
  id: number;
  planId: number;
  categoryId: number | null;
  categoryName: string | null;
  description: string;
  plannedDate: string;
  plannedAmount: number;
}

export interface PlanItemRequest {
  categoryId: number | null;
  description: string;
  plannedDate: string;
  plannedAmount: number;
}
