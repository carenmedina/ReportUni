import { Component, OnDestroy, computed, effect, inject, input, output, signal } from '@angular/core';
import { HistorialItem, descripcionCambio } from '../../../core/models/seguimiento.model';
import { formatearFecha } from '../../../core/models/reporte-consulta.model';
import { SeguimientoService } from '../../../core/services/seguimiento.service';

/**
 * Fase 2: actualizaciones del proceso de un reporte. Muestra, en orden cronologico, cada cambio
 * de estado, asignacion o novedad con la explicacion del administrador y sus fotos de evidencia.
 */
@Component({
  selector: 'app-actualizaciones-reporte',
  standalone: true,
  templateUrl: './actualizaciones-reporte.html',
  styleUrl: './actualizaciones-reporte.scss',
})
export class ActualizacionesReporte implements OnDestroy {
  private readonly seguimiento = inject(SeguimientoService);

  readonly reporteId = input.required<number>();
  readonly historial = input.required<HistorialItem[]>();
  readonly conError = input(false);
  /** Pide al detalle que muestre una foto en grande. */
  readonly ampliar = output<string>();

  // "<id del historial>-<id de la foto>" -> URL temporal (blob:)
  readonly fotosUrl = signal<Record<string, string>>({});
  readonly fotosConError = signal<Record<string, boolean>>({});

  // El servidor las entrega de la mas reciente a la mas antigua; aqui se leen de arriba hacia abajo
  readonly cronologico = computed(() => [...this.historial()].reverse());

  readonly descripcionCambio = descripcionCambio;

  private readonly pedidas = new Set<string>();
  private readonly urlsCreadas: string[] = [];

  constructor() {
    effect(() => {
      const reporteId = this.reporteId();
      for (const item of this.historial()) {
        for (const foto of item.fotos) {
          const clave = `${item.id}-${foto.id}`;
          if (this.pedidas.has(clave)) {
            continue;
          }
          this.pedidas.add(clave);
          this.seguimiento.fotoHistorial(reporteId, item.id, foto.id).subscribe({
            next: (blob) => {
              const url = URL.createObjectURL(blob);
              this.urlsCreadas.push(url);
              this.fotosUrl.update((actual) => ({ ...actual, [clave]: url }));
            },
            error: () => this.fotosConError.update((actual) => ({ ...actual, [clave]: true })),
          });
        }
      }
    });
  }

  iniciales(nombre: string): string {
    return nombre
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((parte) => parte[0]?.toUpperCase())
      .join('');
  }

  fecha(iso: string): string {
    return formatearFecha(iso, true);
  }

  ngOnDestroy(): void {
    this.urlsCreadas.forEach((url) => URL.revokeObjectURL(url));
  }
}