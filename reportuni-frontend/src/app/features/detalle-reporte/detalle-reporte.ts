import { Component, OnDestroy, inject, signal } from '@angular/core';
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

@Component({
  selector: 'app-detalle-reporte',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './detalle-reporte.html',
  styleUrl: './detalle-reporte.scss',
})
export class DetalleReporte implements OnDestroy {
  private readonly servicio = inject(ReporteConsultaService);
  private readonly ruta = inject(ActivatedRoute);

  readonly reporte = signal<ReporteDetalle | null>(null);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  // id de foto -> URL temporal (blob:) para mostrarla; 'error' si no se pudo cargar
  readonly fotosUrl = signal<Record<number, string>>({});
  readonly fotosConError = signal<Record<number, boolean>>({});
  readonly fotoAmpliada = signal<string | null>(null);

  readonly etiquetaEstado = etiquetaEstado;
  readonly etiquetaPrioridad = etiquetaPrioridad;
  readonly etiquetaTipoDano = etiquetaTipoDano;
  readonly ubicacionTexto = ubicacionTexto;

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
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.servicio.interpretarError(err));
        this.cargando.set(false);
      },
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