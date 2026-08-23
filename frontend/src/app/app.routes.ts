import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Register } from './pages/register/register';
import { Dashboard } from './pages/dashboard/dashboard';
import { authGuard } from './guards/auth.guard';
import { Categories } from './pages/categories/categories';
import { CategoryForm } from './pages/categories/category-form/category-form';
import { Expenses } from './pages/expenses/expenses';
import { ExpenseForm } from './pages/expenses/expense-form/expense-form';
import { SavingPlan } from './pages/saving-plan/saving-plan';
import { SavingPlanForm } from './pages/saving-plan/saving-plan-form/saving-plan-form';
import { Plans } from './pages/plans/plans';
import { PlanForm } from './pages/plans/plan-form/plan-form';
import { PlanDetail } from './pages/plans/plan-detail/plan-detail';


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
  { path: 'plans', component: Plans, canActivate: [authGuard] },
  { path: 'plans/create', component: PlanForm, canActivate: [authGuard] },
  { path: 'plans/edit/:id', component: PlanForm, canActivate: [authGuard] },
  { path: 'plans/:id', component: PlanDetail, canActivate: [authGuard] },
  { path: 'saving-plan', component: SavingPlan, canActivate: [authGuard] },
  { path: 'saving-plan/create', component: SavingPlanForm, canActivate: [authGuard] },
  { path: 'saving-plan/edit', component: SavingPlanForm, data: { edit: true }, canActivate: [authGuard] },
  { path: '', pathMatch: 'full', redirectTo: '/dashboard' },
  { path: '**', redirectTo: '/dashboard' }
];
