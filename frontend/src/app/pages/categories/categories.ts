import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CategoryService } from '../../services/category.service';
import {  mergeMap, ReplaySubject } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { CategoryRequest } from '../../models/category';
import { Spinner } from '../../shared/spinner/spinner';

@Component({
  selector: 'app-categories',
  imports: [RouterLink, Spinner],
  templateUrl: './categories.html',
  styleUrl: './categories.css'
})
export class Categories implements OnInit {
  service = inject(CategoryService);
  route = inject(ActivatedRoute);
  reload$ = new ReplaySubject<void>();


  categories = toSignal(
    this.reload$.pipe(mergeMap(() => this.service.getCategoriesResult())),
    { initialValue: { data: undefined, loading: true } }
  );

  ngOnInit(): void {
    this.reload$.next();
  }
  onDelete(id: number) {
    this.service.delete(id).subscribe(() => this.reload$.next());
  }
}
