import { inject, Service } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { Result } from '../models/result';
import { Category, CategoryRequest } from '../models/category';

@Service()
export class CategoryService {
  http = inject(HttpClient);

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(`/api/categories`);
  }

  getCategoriesResult(): Observable<Result<Category[]>> {
    return this.http.get<Category[]>(`/api/categories`).pipe
    (map((result) => ({ data: result, loading: false })));
  }

  save(request: CategoryRequest) {
    return this.http.post<Category>(`/api/categories`, request);
  }

  update(id: number, request: CategoryRequest) {
    return this.http.put<Category>(`/api/categories/${id}`, request);
  }

  delete(id: number) {
    return this.http.delete(`/api/categories/${id}`);
  }
}
