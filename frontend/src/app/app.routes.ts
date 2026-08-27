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
import { Landing } from './pages/landing/landing';
import { Home } from './pages/home/home';
import { MainLayout } from './layout/main-layout/main-layout';


export const routes: Routes = [
  { path: '', component: Landing },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  {
    path: '',
    component: MainLayout,
    canActivate: [authGuard],
    children: [
      { path: 'home', component: Home },
      { path: 'profile', component: Dashboard },
      { path: 'categories', component: Categories },
      { path: 'categories/create', component: CategoryForm },
      { path: 'categories/edit/:id', component: CategoryForm },
      { path: 'expenses', component: Expenses },
      { path: 'expenses/create', component: ExpenseForm },
      { path: 'expenses/edit/:id', component: ExpenseForm },
      { path: 'plans', component: Plans },
      { path: 'plans/create', component: PlanForm },
      { path: 'plans/edit/:id', component: PlanForm },
      { path: 'plans/:id', component: PlanDetail },
      { path: 'saving-plan', component: SavingPlan },
      { path: 'saving-plan/create', component: SavingPlanForm },
      { path: 'saving-plan/edit', component: SavingPlanForm, data: { edit: true } },
    ],
  },
  { path: '**', redirectTo: '/home' }
];
