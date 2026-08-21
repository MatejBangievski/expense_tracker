import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Register } from './pages/register/register';
import { Dashboard } from './pages/dashboard/dashboard';
import { authGuard } from './guards/auth.guard';
import { Categories } from './pages/categories/categories';
import { CategoryForm } from './pages/categories/category-form/category-form';
import { Expenses } from './pages/expenses/expenses';
import { ExpenseForm } from './pages/expenses/expense-form/expense-form';


export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'dashboard', component: Dashboard, canActivate: [authGuard] },
  { path: 'categories', component: Categories, canActivate: [authGuard] },
  { path: 'categories/create', component: CategoryForm, canActivate: [authGuard] },
  { path: 'categories/edit/:id', component: CategoryForm, canActivate: [authGuard] },
  { path: 'expenses', component: Expenses, canActivate: [authGuard] },
  { path: 'expenses/create', component: ExpenseForm, canActivate: [authGuard] },
  { path: 'expenses/edit/:id', component: ExpenseForm, canActivate: [authGuard] },
  { path: '', pathMatch: 'full', redirectTo: '/dashboard' },
  { path: '**', redirectTo: '/dashboard' }
];
