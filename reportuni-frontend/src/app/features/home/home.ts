import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { REPORTES_EJEMPLO } from '../../core/models/reportes-mock.data';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  private readonly authService = inject(AuthService);

  readonly usuario = this.authService.usuarioActual;
  readonly ultimosReportes = REPORTES_EJEMPLO.slice(0, 4);

  readonly totales = {
    total: REPORTES_EJEMPLO.length,
    pendientes: REPORTES_EJEMPLO.filter((r) => r.estado === 'PENDIENTE').length,
    enProceso: REPORTES_EJEMPLO.filter((r) => r.estado === 'EN_PROCESO').length,
    resueltos: REPORTES_EJEMPLO.filter((r) => r.estado === 'RESUELTO').length,
  };
}