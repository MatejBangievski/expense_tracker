export interface Category {
  id: number;
  name: string;
  parentCategoryId: number | null;
  parentCategoryName: string | null;
  default: boolean;
}

export interface CategoryRequest {
  name: string;
  parentCategoryId?: number;
}
