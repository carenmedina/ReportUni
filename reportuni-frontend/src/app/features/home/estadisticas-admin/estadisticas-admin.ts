import { Component, computed, inject, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { ConteoEstadistica, Estadisticas, RangoFechas } from '../../../core/models/estadisticas.model';
import { etiquetaBloque, etiquetaPrioridad, etiquetaTipoDano } from '../../../core/models/reporte-consulta.model';
import { PrioridadReporte } from '../../../core/models/reporte.model';
import { EstadisticasService } from '../../../core/services/estadisticas.service';

export interface BarraRanking {
  etiqueta: string;
  total: number;
  porcentajeDelTotal: number;
  anchoBarra: number;
}

export interface SegmentoDona {
  codigo: string;
  etiqueta: string;
  total: number;
  porcentaje: number;
  longitud: number;
  desplazamiento: number;
  color: string;
}

type Preset = 'TODO' | '30' | '90' | 'PERSONALIZADO';

const COLORES_PRIORIDAD: Record<string, string> = {
  ALTA: '#c0392b',
  MEDIA: '#d99a1f',
  BAJA: '#47a447',
};

function aTexto(fecha: Date): string {
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${mes}-${dia}`;
}

@Component({
  selector: 'app-estadisticas-admin',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './estadisticas-admin.html',
  styleUrl: './estadisticas-admin.scss',
})
export class EstadisticasAdmin implements OnDestroy {
  private readonly servicio = inject(EstadisticasService);

  readonly rango = signal<RangoFechas>({ desde: '', hasta: '' });
  readonly preset = signal<Preset>('TODO');
  readonly datos = signal<Estadisticas | null>(null);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  readonly hoy = aTexto(new Date());

  readonly rankingTipo = computed(() => this.construirRanking(this.datos()?.porTipo ?? [], etiquetaTipoDano));
  readonly rankingBloque = computed(() => this.construirRanking(this.datos()?.porBloque ?? [], etiquetaBloque));
  readonly segmentos = computed(() => this.construirDona(this.datos()?.porPrioridad ?? []));

  readonly promedioTexto = computed(() => {
    const dias = this.datos()?.promedioResolucionDias;
    if (dias === null || dias === undefined) {
      return null;
    }
    return `${dias.toLocaleString('es-CO', { maximumFractionDigits: 1 })} ${dias === 1 ? 'día' : 'días'}`;
  });

  private peticion?: Subscription;

  constructor() {
    this.cargar();
  }

  ngOnDestroy(): void {
    this.peticion?.unsubscribe();
  }

  elegirPreset(preset: Preset): void {
    this.preset.set(preset);
    if (preset === 'TODO') {
      this.rango.set({ desde: '', hasta: '' });
    } else if (preset === '30' || preset === '90') {
      const dias = Number(preset);
      const desde = new Date();
      desde.setDate(desde.getDate() - (dias - 1));
      this.rango.set({ desde: aTexto(desde), hasta: aTexto(new Date()) });
    } else {
      return;
    }
    this.cargar();
  }

  cambiarFecha(campo: 'desde' | 'hasta', valor: string): void {
    this.preset.set('PERSONALIZADO');
    this.rango.update((r) => ({ ...r, [campo]: valor }));
    this.cargar();
  }

  cargar(): void {
    this.peticion?.unsubscribe();
    this.cargando.set(true);
    this.error.set(null);

    this.peticion = this.servicio.obtener(this.rango()).subscribe({
      next: (datos) => {
        this.datos.set(datos);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.servicio.interpretarError(err));
        this.cargando.set(false);
      },
    });
  }

  private construirRanking(conteos: ConteoEstadistica[], etiqueta: (codigo: string) => string): BarraRanking[] {
    const total = this.datos()?.totalReportes ?? 0;
    const maximo = Math.max(0, ...conteos.map((c) => c.total));
    return conteos.map((c) => ({
      etiqueta: etiqueta(c.codigo),
      total: c.total,
      porcentajeDelTotal: total > 0 ? Math.round((c.total / total) * 100) : 0,
      anchoBarra: maximo > 0 ? (c.total / maximo) * 100 : 0,
    }));
  }

  /** La dona se dibuja con un circulo de circunferencia 100: cada tramo mide su porcentaje. */
  private construirDona(conteos: ConteoEstadistica[]): SegmentoDona[] {
    const total = conteos.reduce((suma, c) => suma + c.total, 0);
    let acumulado = 0;
    return conteos.map((c) => {
      const porcentaje = total > 0 ? (c.total / total) * 100 : 0;
      const segmento: SegmentoDona = {
        codigo: c.codigo,
        etiqueta: etiquetaPrioridad(c.codigo as PrioridadReporte),
        total: c.total,
        porcentaje: Math.round(porcentaje),
        longitud: porcentaje,
        desplazamiento: 25 - acumulado,
        color: COLORES_PRIORIDAD[c.codigo] ?? '#8a8f91',
      };
      acumulado += porcentaje;
      return segmento;
    });
  }
}