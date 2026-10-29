import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  FotoInfo,
  ReporteDetalle,
  etiquetaEstado,
  etiquetaPrioridad,
  etiquetaTipoDano,
  formatearFecha,
  ubicacionTexto,
} from '../../core/models/reporte-consulta.model';
import { ReporteConsultaService } from '../../core/services/reporte-consulta.service';
import { SeguimientoService } from '../../core/services/seguimiento.service';
import { HistorialItem, etiquetaArea, etiquetaValor } from '../../core/models/seguimiento.model';
import { GestionReporte } from './gestion-reporte/gestion-reporte';
import { ActualizacionesReporte } from './actualizaciones-reporte/actualizaciones-reporte';

interface PasoLineaTiempo {
  titulo: string;
  detalle: string;
  estado: 'hecho' | 'actual' | 'pendiente';
}

@Component({
  selector: 'app-detalle-reporte',
  standalone: true,
  imports: [CommonModule, RouterLink, GestionReporte, ActualizacionesReporte],
  templateUrl: './detalle-reporte.html',
  styleUrl: './detalle-reporte.scss',
})
export class DetalleReporte implements OnDestroy {
  private readonly servicio = inject(ReporteConsultaService);
  private readonly seguimiento = inject(SeguimientoService);
  private readonly ruta = inject(ActivatedRoute);

  readonly volverA: string = this.ruta.snapshot.data['volverA'] ?? '/mis-reportes';
  readonly volverTexto: string = this.ruta.snapshot.data['volverTexto'] ?? 'Mis reportes';

  readonly gestionar: boolean = this.ruta.snapshot.data['gestionar'] === true;

  readonly reporte = signal<ReporteDetalle | null>(null);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  readonly historial = signal<HistorialItem[]>([]);
  readonly historialError = signal(false);

  readonly fotosUrl = signal<Record<number, string>>({});
  readonly fotosConError = signal<Record<number, boolean>>({});
  readonly fotoAmpliada = signal<string | null>(null);

  readonly etiquetaEstado = etiquetaEstado;
  readonly etiquetaPrioridad = etiquetaPrioridad;
  readonly etiquetaTipoDano = etiquetaTipoDano;
  readonly ubicacionTexto = ubicacionTexto;
  readonly etiquetaArea = etiquetaArea;

  readonly pasos = computed<PasoLineaTiempo[]>(() => {
    const r = this.reporte();
    if (!r) {
      return [];
    }
    const antiguosPrimero = [...this.historial()].reverse();
    const pasos: PasoLineaTiempo[] = [
      { titulo: 'Reporte enviado', detalle: formatearFecha(r.fechaCreacion, true), estado: 'hecho' },
    ];

    for (const h of antiguosPrimero.filter((x) => x.tipo === 'ASIGNACION')) {
      pasos.push({
        titulo: `${h.valorAnterior ? 'Reasignado a' : 'Asignando a'} ${etiquetaValor(h.valorNuevo)}`,
        detalle: formatearFecha(h.fecha, true),
        estado: 'hecho',
      });
    }

    const ultimaVez = (estado: string) =>
      [...antiguosPrimero].reverse().find((h) => h.tipo === 'ESTADO' && h.valorNuevo === estado);

    const aProceso = ultimaVez('EN_PROCESO');
    const estadoProceso = r.estado === 'EN_PROCESO' ? 'actual' : r.estado === 'RESUELTO' ? 'hecho' : 'pendiente';
    pasos.push({
      titulo: 'En proceso',
      detalle:
        estadoProceso !== 'pendiente' && aProceso ? `Desde el ${formatearFecha(aProceso.fecha, true)}` : 'Pendiente',
      estado: estadoProceso,
    });

    const aResuelto = ultimaVez('RESUELTO');
    pasos.push({
      titulo: 'Resuelto',
      detalle: r.estado === 'RESUELTO' && aResuelto ? formatearFecha(aResuelto.fecha, true) : 'Pendiente',
      estado: r.estado === 'RESUELTO' ? 'hecho' : 'pendiente',
    });

    return pasos;
  });

  private urlsCreadas: string[] = [];

  constructor() {
    const id = Number(this.ruta.snapshot.paramMap.get('id'));
    if (!Number.isInteger(id) || id <= 0) {
      this.error.set('No encontramos ese reporte.');
      this.cargando.set(false);
      return;
    }
    this.cargar(id);
  }

  cargar(id: number): void {
    this.cargando.set(true);
    this.error.set(null);

    this.servicio.obtenerDetalle(id).subscribe({
      next: (reporte) => {
        this.reporte.set(reporte);
        this.cargando.set(false);
        reporte.fotos.forEach((foto) => this.cargarFoto(reporte.id, foto));
        this.cargarHistorial(reporte.id);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.servicio.interpretarError(err));
        this.cargando.set(false);
      },
    });
  }

  refrescar(): void {
    const actual = this.reporte();
    if (!actual) {
      return;
    }
    this.servicio.obtenerDetalle(actual.id).subscribe({
      next: (reporte) => this.reporte.set(reporte),
      error: () => undefined,
    });
    this.cargarHistorial(actual.id);
  }

  private cargarHistorial(id: number): void {
    this.historialError.set(false);
    this.seguimiento.historial(id).subscribe({
      next: (lista) => this.historial.set(lista),
      error: () => this.historialError.set(true),
    });
  }

  private cargarFoto(reporteId: number, foto: FotoInfo): void {
    this.servicio.obtenerFoto(reporteId, foto.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        this.urlsCreadas.push(url);
        this.fotosUrl.update((actual) => ({ ...actual, [foto.id]: url }));
      },
      error: () => this.fotosConError.update((actual) => ({ ...actual, [foto.id]: true })),
    });
  }

  ampliar(url: string | undefined): void {
    if (url) {
      this.fotoAmpliada.set(url);
    }
  }

  cerrarAmpliada(): void {
    this.fotoAmpliada.set(null);
  }

  fecha(reporte: ReporteDetalle): string {
    return formatearFecha(reporte.fechaCreacion, true);
  }

  ultimaActualizacion(reporte: ReporteDetalle): string {
    return formatearFecha(reporte.ultimaActualizacion, true);
  }

  tieneUbicacionGps(reporte: ReporteDetalle): boolean {
    return reporte.latitud !== null && reporte.longitud !== null;
  }

  enlaceMapa(reporte: ReporteDetalle): string {
    return `https://www.openstreetmap.org/?mlat=${reporte.latitud}&mlon=${reporte.longitud}#map=18/${reporte.latitud}/${reporte.longitud}`;
  }

  tamano(bytes: number): string {
    return bytes >= 1024 * 1024
      ? `${(bytes / (1024 * 1024)).toFixed(1)} MB`
      : `${Math.max(1, Math.round(bytes / 1024))} KB`;
  }

  ngOnDestroy(): void {
    this.urlsCreadas.forEach((url) => URL.revokeObjectURL(url));
  }
}