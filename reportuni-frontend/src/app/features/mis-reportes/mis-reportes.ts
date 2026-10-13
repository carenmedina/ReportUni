import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { EstadoReporte } from '../../core/models/reporte.model';
import {
  ReporteResumen,
  etiquetaEstado,
  etiquetaPrioridad,
  etiquetaTipoDano,
  formatearFecha,
  ubicacionTexto,
} from '../../core/models/reporte-consulta.model';
import { ReporteConsultaService } from '../../core/services/reporte-consulta.service';

@Component({
  selector: 'app-mis-reportes',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './mis-reportes.html',
  styleUrl: './mis-reportes.scss',
})
export class MisReportes {
  private readonly servicio = inject(ReporteConsultaService);

  readonly busqueda = signal('');
  readonly filtroEstado = signal<EstadoReporte | 'TODOS'>('TODOS');

  readonly reportes = signal<ReporteResumen[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  readonly reportesFiltrados = computed(() => {
    const texto = this.busqueda().trim().toLowerCase();
    const estado = this.filtroEstado();

    return this.reportes().filter((r) => {
      const coincideEstado = estado === 'TODOS' || r.estado === estado;
      const coincideTexto =
        !texto ||
        r.codigo.toLowerCase().includes(texto) ||
        etiquetaTipoDano(r.tipoDano).toLowerCase().includes(texto) ||
        this.ubicacion(r).toLowerCase().includes(texto);
      return coincideEstado && coincideTexto;
    });
  });

  readonly etiquetaEstado = etiquetaEstado;
  readonly etiquetaPrioridad = etiquetaPrioridad;
  readonly etiquetaTipoDano = etiquetaTipoDano;

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);

    this.servicio.listarMios().subscribe({
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

  ubicacion(reporte: ReporteResumen): string {
    return ubicacionTexto(reporte.bloque, reporte.espacioEspecifico);
  }

  fecha(reporte: ReporteResumen): string {
    return formatearFecha(reporte.fechaCreacion);
  }

  actualizarBusqueda(valor: string): void {
    this.busqueda.set(valor);
  }

  actualizarFiltroEstado(valor: string): void {
    this.filtroEstado.set(valor as EstadoReporte | 'TODOS');
  }
}