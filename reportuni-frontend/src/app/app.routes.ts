import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

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
      {
        path: '',
        pathMatch: 'full',
        loadComponent: () => import('./core/guards/entrada-redirect').then((m) => m.EntradaRedirect),
      },
      {
        path: 'inicio',
        canActivate: [roleGuard(['ADMINISTRADOR'])],
        loadComponent: () => import('./features/home/home').then((m) => m.Home),
      },
      {
        path: 'mis-reportes',
        canActivate: [roleGuard(['ESTUDIANTE'])],
        loadComponent: () => import('./features/mis-reportes/mis-reportes').then((m) => m.MisReportes),
      },
      {
        path: 'nuevo-reporte',
        canActivate: [roleGuard(['ESTUDIANTE'])],
        loadComponent: () => import('./features/nuevo-reporte/nuevo-reporte').then((m) => m.NuevoReporte),
      },
      {
        path: 'admin/reportes',
        canActivate: [roleGuard(['ADMINISTRADOR'])],
        loadComponent: () => import('./features/admin-reportes/admin-reportes').then((m) => m.AdminReportes),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];