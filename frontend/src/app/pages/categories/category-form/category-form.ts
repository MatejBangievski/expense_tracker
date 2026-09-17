import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { form, FormField, FormRoot, required } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { CategoryService } from '../../../services/category.service';
import { firstValueFrom, map, mergeMap, of } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { Category } from '../../../models/category';

@Component({
  selector: 'app-category-form',
  imports: [FormField, FormRoot, RouterLink],
  templateUrl: './category-form.html',
  styleUrl: '../../form-page.css',
})
export class CategoryForm implements OnInit {
  service = inject(CategoryService);
  router = inject(Router);
  route = inject(ActivatedRoute);

  existingCategory = signal<Category | undefined>(undefined);

  categories = signal<Category[]>([]);

  categoryModel = signal<CategoryFormModel>({
    name: '',
    parentCategoryId: '0',
  });

  selectedParent = computed(() =>
    this.categories().find(
      (category) => String(category.id) === this.categoryModel().parentCategoryId,
    ),
  );

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

          const existing = this.existingCategory();
          if (existing) {
            await firstValueFrom(this.service.update(existing.id, request));
          } else {
            await firstValueFrom(this.service.save(request));
          }
          await this.router.navigate(['/categories']);
        },
      },
    },
  );

  ngOnInit(): void {
    this.service.getCategories().subscribe((categories) => this.categories.set(categories));

    this.route.paramMap
      .pipe(
        map((params) => params.get('id')),
        mergeMap((id) =>
          id
            ? this.service.getCategories().pipe(map((all) => all.find((c) => c.id === +id)))
            : of(undefined),
        ),
      )
      .subscribe((category) => {
        if (category) {
          this.categoryModel.set({
            name: category.name,
            parentCategoryId: category.parentCategoryId ? String(category.parentCategoryId) : '0',
          });
          this.existingCategory.set(category);
        }
      });
  }
}

interface CategoryFormModel {
  name: string;
  parentCategoryId: string;
}
