import { Component, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import {
  AdminReporteItem,
  FILTROS_VACIOS,
  FiltrosAdmin,
  OPCIONES_BLOQUE,
  OPCIONES_TIPO_DANO,
} from '../../core/models/admin-reporte.model';
import {
  etiquetaEstado,
  etiquetaPrioridad,
  etiquetaTipoDano,
  formatearFecha,
  ubicacionTexto,
} from '../../core/models/reporte-consulta.model';
import { AdminReporteService } from '../../core/services/admin-reporte.service';

@Component({
  selector: 'app-admin-reportes',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-reportes.html',
  styleUrl: './admin-reportes.scss',
})
export class AdminReportes implements OnDestroy {
  private readonly servicio = inject(AdminReporteService);

  readonly filtros = signal<FiltrosAdmin>({ ...FILTROS_VACIOS });
  readonly reportes = signal<AdminReporteItem[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  readonly opcionesTipo = OPCIONES_TIPO_DANO;
  readonly opcionesBloque = OPCIONES_BLOQUE;

  readonly etiquetaEstado = etiquetaEstado;
  readonly etiquetaPrioridad = etiquetaPrioridad;
  readonly etiquetaTipoDano = etiquetaTipoDano;

  private peticion?: Subscription;
  private temporizador?: ReturnType<typeof setTimeout>;

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.peticion?.unsubscribe();
    this.cargando.set(true);
    this.error.set(null);

    this.peticion = this.servicio.listar(this.filtros()).subscribe({
      next: (lista) => {
        this.reportes.set(lista);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.servicio.interpretarError(err));
        this.cargando.set(false);
      },
    });
  }

  // El texto se envia al servidor con una pequena espera para no pedir en cada tecla
  cambiarBusqueda(valor: string): void {
    this.filtros.update((f) => ({ ...f, q: valor }));
    clearTimeout(this.temporizador);
    this.temporizador = setTimeout(() => this.cargar(), 300);
  }

  cambiarFiltro(campo: 'estado' | 'prioridad' | 'tipoDano' | 'bloque', valor: string): void {
    this.filtros.update((f) => ({ ...f, [campo]: valor }));
    this.cargar();
  }

  hayFiltros(): boolean {
    const f = this.filtros();
    return (
      f.q.trim() !== '' ||
      f.estado !== 'TODOS' ||
      f.prioridad !== 'TODAS' ||
      f.tipoDano !== 'TODOS' ||
      f.bloque !== 'TODOS'
    );
  }

  limpiarFiltros(): void {
    clearTimeout(this.temporizador);
    this.filtros.set({ ...FILTROS_VACIOS });
    this.cargar();
  }

  ubicacion(reporte: AdminReporteItem): string {
    return ubicacionTexto(reporte.bloque, reporte.espacioEspecifico);
  }

  fecha(reporte: AdminReporteItem): string {
    return formatearFecha(reporte.fechaCreacion);
  }

  ngOnDestroy(): void {
    clearTimeout(this.temporizador);
    this.peticion?.unsubscribe();
  }
}