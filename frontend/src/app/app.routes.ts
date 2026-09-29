import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'work-orders' },
  {
    path: 'login',
    loadComponent: () => import('./features/login/login').then((m) => m.Login),
    title: 'Sign in · FieldOps',
  },
  {
    path: 'work-orders',
    loadComponent: () =>
      import('./features/work-orders/work-order-list/work-order-list').then((m) => m.WorkOrderList),
    title: 'Work orders · FieldOps',
  },
  {
    path: 'work-orders/new',
    loadComponent: () =>
      import('./features/work-orders/work-order-create/work-order-create').then(
        (m) => m.WorkOrderCreate,
      ),
    title: 'New work order · FieldOps',
  },
  {
    path: 'work-orders/:id',
    loadComponent: () =>
      import('./features/work-orders/work-order-detail/work-order-detail').then(
        (m) => m.WorkOrderDetail,
      ),
    title: 'Work order · FieldOps',
  },
  { path: '**', redirectTo: 'work-orders' },
];
