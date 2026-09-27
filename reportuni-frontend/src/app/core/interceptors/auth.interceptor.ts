import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Agrega el header "Authorization: Bearer <token>" a cada peticion hacia
 * la API cuando hay una sesion activa, y redirige al login si el backend
 * responde 401 (token vencido o invalido).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authService.obtenerToken();
  const esPeticionApi = req.url.includes('/api/');

  const peticion =
    token && esPeticionApi
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(peticion).pipe(
    catchError((error) => {
      if (error?.status === 401 && esPeticionApi) {
        authService.logout();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
