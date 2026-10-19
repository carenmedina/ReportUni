import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { AdminReporteService } from '../../core/services/admin-reporte.service';
import { AdminResumen } from '../../core/models/admin-reporte.model';
import {
  etiquetaEstado,
  etiquetaTipoDano,
  formatearFecha,
  ubicacionTexto,
} from '../../core/models/reporte-consulta.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  private readonly authService = inject(AuthService);
  private readonly servicio = inject(AdminReporteService);

  readonly usuario = this.authService.usuarioActual;

  readonly resumen = signal<AdminResumen | null>(null);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  readonly etiquetaEstado = etiquetaEstado;
  readonly etiquetaTipoDano = etiquetaTipoDano;
  readonly ubicacionTexto = ubicacionTexto;
  readonly formatearFecha = formatearFecha;

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);

    this.servicio.resumen().subscribe({
      next: (resumen) => {
        this.resumen.set(resumen);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.servicio.interpretarError(err));
        this.cargando.set(false);
      },
    });
  }
}