import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Rol } from '../models/usuario.model';

export function rutaPorDefecto(rol: Rol | undefined): string {
  return rol === 'ADMINISTRADOR' ? '/inicio' : '/mis-reportes';
}

export const roleGuard = (rolesPermitidos: Rol[]): CanActivateFn => {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);
    const usuario = authService.usuarioActual();

    if (usuario && rolesPermitidos.includes(usuario.rol)) {
      return true;
    }

    router.navigate([rutaPorDefecto(usuario?.rol)]);
    return false;
  };
};