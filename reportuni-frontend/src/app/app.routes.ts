import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login').then((m) => m.Login),
  },
  {
    path: '',
    loadComponent: () => import('./shared/layout/layout').then((m) => m.Layout),
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      {
        path: 'inicio',
        loadComponent: () => import('./features/home/home').then((m) => m.Home),
      },
      {
        path: 'mis-reportes',
        loadComponent: () => import('./features/mis-reportes/mis-reportes').then((m) => m.MisReportes),
      },
      {
        path: 'nuevo-reporte',
        loadComponent: () => import('./features/nuevo-reporte/nuevo-reporte').then((m) => m.NuevoReporte),
      },
      {
        path: 'admin/reportes',
        loadComponent: () => import('./features/admin-reportes/admin-reportes').then((m) => m.AdminReportes),
      },
    ],
  },
  { path: '**', redirectTo: 'inicio' },
];
