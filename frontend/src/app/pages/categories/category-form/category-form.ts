import { Component, inject, OnInit, signal } from '@angular/core';
import { form, FormField, FormRoot, required } from '@angular/forms/signals';
import { CategoryService } from '../../../services/category.service';
import { firstValueFrom, map, mergeMap, of } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { Category } from '../../../models/category';

@Component({
  selector: 'app-category-form',
  imports: [FormField, FormRoot],
  templateUrl: './category-form.html',
  styleUrl: './category-form.css',
})
export class CategoryForm implements OnInit {
  service = inject(CategoryService);
  router = inject(Router);
  route = inject(ActivatedRoute);

  existingCategory: Category | undefined;

  categories = signal<Category[]>([]);

  categoryModel = signal<CategoryFormModel>({
    name: '',
    parentCategoryId: '0',
  });

  categoryForm = form(
    this.categoryModel,
    (schemaPath) => {
      required(schemaPath.name, { message: 'Name is required' });
    },
    {
      submission: {
        action: async (form) => {
          const value = form().value();
          const request = {
            name: value.name,
            parentCategoryId: value.parentCategoryId !== '0' ? +value.parentCategoryId : undefined,
          };

          let result;
          if (this.existingCategory) {
            result = await firstValueFrom(this.service.update(this.existingCategory.id, request));
          } else {
            result = await firstValueFrom(this.service.save(request));
          }
          console.log('result', result);
          await this.router.navigate(['/categories']);
          return;
        },
      },
    },
  );

  ngOnInit(): void {
    this.service.getCategories().subscribe((categories) => this.categories.set(categories));

    this.route.paramMap
      .pipe(
        map((params) => params.get('id')),
        mergeMap((id) => {
          if (id) {
            return this.service.getCategories().pipe(
              map((all) => all.find((c) => c.id === +id)),
            );
          } else {
            return of(undefined);
          }
        }),
      )
      .subscribe((category) => {
        if (category) {
          this.categoryModel.set({
            name: category.name,
            parentCategoryId: category.parentCategoryId ? String(category.parentCategoryId) : '0',
          });
          this.existingCategory = category;
        }
      });
  }
}

interface CategoryFormModel {
  name: string;
  parentCategoryId: string;
}
