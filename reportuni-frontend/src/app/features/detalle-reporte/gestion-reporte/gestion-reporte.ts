import { Component, computed, inject, input, output, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AdminReporteService } from '../../../core/services/admin-reporte.service';
import { AdminReporteItem, OPCIONES_ESTADO, OPCIONES_PRIORIDAD } from '../../../core/models/admin-reporte.model';
import { ReporteDetalle, etiquetaEstado } from '../../../core/models/reporte-consulta.model';
import { OPCIONES_AREA, etiquetaValor } from '../../../core/models/seguimiento.model';
import { EstadoReporte, PrioridadReporte } from '../../../core/models/reporte.model';
import { SelectorFotos } from '../../../shared/selector-fotos/selector-fotos';

type Seccion = 'prioridad' | 'asignacion' | 'estado' | 'actualizacion';

const NOTA_MIN = 10;
const NOTA_MAX = 1000;

/**
 * Fase 2: panel del administrador para gestionar un reporte con evidencia. Cambiar el estado exige
 * explicar que se hace o se hizo (y fotos del arreglo al resolver); tambien permite asignar el
 * reporte a un equipo y publicar novedades del proceso sin cambiar el estado.
 */
@Component({
  selector: 'app-gestion-reporte',
  standalone: true,
  imports: [SelectorFotos],
  templateUrl: './gestion-reporte.html',
  styleUrl: './gestion-reporte.scss',
})
export class GestionReporte {
  private readonly servicio = inject(AdminReporteService);

  readonly reporte = input.required<ReporteDetalle>();
  /** Se emite cuando el servidor guardo un cambio, para que el detalle se vuelva a cargar. */
  readonly cambiado = output<void>();

  readonly opcionesPrioridad = OPCIONES_PRIORIDAD;
  readonly opcionesArea = OPCIONES_AREA;

  readonly guardando = signal<Seccion | null>(null);
  readonly aviso = signal<{ seccion: Seccion; tipo: 'ok' | 'error'; texto: string } | null>(null);

  // asignacion
  readonly areaElegida = signal('');
  readonly areaMostrada = computed(() => this.areaElegida() || this.reporte().asignadoA || '');

  // cambio de estado
  readonly estadoElegido = signal<EstadoReporte | ''>('');
  readonly notaEstado = signal('');
  readonly fotosEstado = signal<File[]>([]);
  readonly estadosDisponibles = computed(() => OPCIONES_ESTADO.filter((o) => o.valor !== this.reporte().estado));
  readonly fotosObligatorias = computed(() => this.estadoElegido() === 'RESUELTO');
  readonly etiquetaNotaEstado = computed(() => {
    switch (this.estadoElegido()) {
      case 'RESUELTO':
        return '¿Qué se hizo para resolver el reporte?';
      case 'PENDIENTE':
        return 'Motivo para devolver el reporte a Pendiente';
      default:
        return '¿Qué se está haciendo para atender el reporte?';
    }
  });

  // actualizaciones del proceso
  readonly notaActualizacion = signal('');
  readonly fotosActualizacion = signal<File[]>([]);

  readonly ocupado = computed(() => this.guardando() !== null);

  avisoDe(seccion: Seccion): { tipo: 'ok' | 'error'; texto: string } | null {
    const actual = this.aviso();
    return actual && actual.seccion === seccion ? actual : null;
  }

  cambiarPrioridad(selector: HTMLSelectElement): void {
    const nueva = selector.value as PrioridadReporte;
    if (nueva === this.reporte().prioridadEstimada) {
      return;
    }
    this.enviar(
      'prioridad',
      this.servicio.cambiarPrioridad(this.reporte().id, nueva),
      `Prioridad actualizada a «${etiquetaValor(nueva)}».`,
      () => (selector.value = this.reporte().prioridadEstimada),
    );
  }

  asignar(): void {
    const area = this.areaElegida();
    if (!area) {
      this.error('asignacion', 'Selecciona a quién se asigna el reporte.');
      return;
    }
    this.enviar(
      'asignacion',
      this.servicio.asignar(this.reporte().id, area),
      `Reporte asignado a ${etiquetaValor(area)}.`,
      undefined,
      () => this.areaElegida.set(''),
    );
  }

  guardarEstado(): void {
    const estado = this.estadoElegido();
    if (!estado) {
      this.error('estado', 'Selecciona el nuevo estado.');
      return;
    }
    const nota = this.notaEstado().trim();
    if (nota.length < NOTA_MIN || nota.length > NOTA_MAX) {
      this.error('estado', `${this.etiquetaNotaEstado()} (entre ${NOTA_MIN} y ${NOTA_MAX} caracteres).`);
      return;
    }
    if (estado === 'RESUELTO' && this.fotosEstado().length === 0) {
      this.error('estado', 'Adjunta al menos una fotografía del arreglo para marcar el reporte como resuelto.');
      return;
    }

    this.enviar(
      'estado',
      this.servicio.cambiarEstado(this.reporte().id, estado, nota, this.fotosEstado()),
      `Estado actualizado a «${etiquetaEstado(estado)}». El estudiante verá la explicación y recibirá un aviso.`,
      undefined,
      () => {
        this.estadoElegido.set('');
        this.notaEstado.set('');
        this.fotosEstado.set([]);
      },
    );
  }

  publicarActualizacion(): void {
    const nota = this.notaActualizacion().trim();
    if (nota.length < NOTA_MIN || nota.length > NOTA_MAX) {
      this.error('actualizacion', `Describe la novedad del proceso (entre ${NOTA_MIN} y ${NOTA_MAX} caracteres).`);
      return;
    }
    this.enviar(
      'actualizacion',
      this.servicio.agregarActualizacion(this.reporte().id, nota, this.fotosActualizacion()),
      'Actualización publicada. El estudiante recibirá un aviso.',
      undefined,
      () => {
        this.notaActualizacion.set('');
        this.fotosActualizacion.set([]);
      },
    );
  }

  private error(seccion: Seccion, texto: string): void {
    this.aviso.set({ seccion, tipo: 'error', texto });
  }

  private enviar(
    seccion: Seccion,
    peticion: Observable<AdminReporteItem>,
    mensajeOk: string,
    alFallar?: () => void,
    alGuardar?: () => void,
  ): void {
    this.guardando.set(seccion);
    this.aviso.set(null);

    peticion.subscribe({
      next: () => {
        alGuardar?.();
        this.guardando.set(null);
        this.aviso.set({ seccion, tipo: 'ok', texto: mensajeOk });
        this.cambiado.emit();
      },
      error: (err: HttpErrorResponse) => {
        alFallar?.();
        this.guardando.set(null);
        this.error(seccion, this.servicio.interpretarError(err));
      },
    });
  }
}