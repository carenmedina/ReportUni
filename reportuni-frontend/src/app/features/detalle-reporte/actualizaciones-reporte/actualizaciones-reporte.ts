import { Component, OnDestroy, computed, effect, inject, input, output, signal, untracked } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FotoHistorial, HistorialItem, descripcionCambio } from '../../../core/models/seguimiento.model';
import { formatearFecha } from '../../../core/models/reporte-consulta.model';
import { Comentario, MAX_CARACTERES_COMENTARIO } from '../../../core/models/comentario.model';
import { SeguimientoService } from '../../../core/services/seguimiento.service';
import { ComentarioService } from '../../../core/services/comentario.service';

interface Entrada {
  clave: string;
  tipo: 'cambio' | 'comentario';
  autor: string;
  propio: boolean;
  esAdmin: boolean;
  fecha: string;
  titulo: string | null;
  texto: string | null;
  historialId: number | null;
  fotos: FotoHistorial[];
}

@Component({
  selector: 'app-actualizaciones-reporte',
  standalone: true,
  templateUrl: './actualizaciones-reporte.html',
  styleUrl: './actualizaciones-reporte.scss',
})
export class ActualizacionesReporte implements OnDestroy {
  static readonly INTERVALO_MS = 30000;

  private readonly seguimiento = inject(SeguimientoService);
  private readonly comentariosServicio = inject(ComentarioService);

  readonly reporteId = input.required<number>();
  readonly historial = input.required<HistorialItem[]>();
  readonly conError = input(false);
  /** Pide al detalle que muestre una foto en grande. */
  readonly ampliar = output<string>();

  readonly comentarios = signal<Comentario[]>([]);
  readonly comentariosError = signal(false);

  readonly texto = signal('');
  readonly enviando = signal(false);
  readonly errorEnvio = signal<string | null>(null);
  readonly limite = MAX_CARACTERES_COMENTARIO;

  readonly fotosUrl = signal<Record<string, string>>({});
  readonly fotosConError = signal<Record<string, boolean>>({});

  readonly entradas = computed<Entrada[]>(() => {
    const cambios: Entrada[] = this.historial().map((h) => ({
      clave: `h${h.id}`,
      tipo: 'cambio',
      autor: h.realizadoPor,
      propio: false,
      esAdmin: true,
      fecha: h.fecha,
      titulo: descripcionCambio(h),
      texto: h.nota,
      historialId: h.id,
      fotos: h.fotos,
    }));
    const mensajes: Entrada[] = this.comentarios().map((c) => ({
      clave: `c${c.id}`,
      tipo: 'comentario',
      autor: c.autor,
      propio: c.propio,
      esAdmin: c.rol === 'ADMINISTRADOR',
      fecha: c.fecha,
      titulo: null,
      texto: c.texto,
      historialId: null,
      fotos: [],
    }));
    return [...cambios, ...mensajes].sort((a, b) => (a.fecha === b.fecha ? 0 : a.fecha < b.fecha ? -1 : 1));
  });

  private readonly pedidas = new Set<string>();
  private readonly urlsCreadas: string[] = [];
  private temporizador: ReturnType<typeof setInterval> | null = null;

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

    effect(() => {
      const reporteId = this.reporteId();
      untracked(() => this.cargarComentarios(reporteId, false));
    });
    this.temporizador = setInterval(() => this.cargarComentarios(this.reporteId(), true), ActualizacionesReporte.INTERVALO_MS);
  }

  reintentar(): void {
    this.cargarComentarios(this.reporteId(), false);
  }

  cambiarTexto(valor: string): void {
    this.texto.set(valor);
    if (this.errorEnvio()) {
      this.errorEnvio.set(null);
    }
  }

  enviar(): void {
    if (this.enviando()) {
      return;
    }
    const contenido = this.texto().trim();
    if (!contenido) {
      this.errorEnvio.set('Escribe un comentario antes de enviarlo.');
      return;
    }
    if (contenido.length > this.limite) {
      this.errorEnvio.set(`El comentario no puede superar los ${this.limite} caracteres.`);
      return;
    }

    this.enviando.set(true);
    this.errorEnvio.set(null);
    this.comentariosServicio.comentar(this.reporteId(), contenido).subscribe({
      next: (creado) => {
        this.comentarios.update((lista) => [...lista, creado]);
        this.texto.set('');
        this.enviando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorEnvio.set(this.comentariosServicio.interpretarError(err));
        this.enviando.set(false);
      },
    });
  }

  private cargarComentarios(reporteId: number, silencioso: boolean): void {
    this.comentariosServicio.listar(reporteId).subscribe({
      next: (lista) => {
        this.comentarios.set(lista);
        this.comentariosError.set(false);
      },
      error: () => {
        if (!silencioso) {
          this.comentariosError.set(true);
        }
      },
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
    if (this.temporizador) {
      clearInterval(this.temporizador);
    }
    this.urlsCreadas.forEach((url) => URL.revokeObjectURL(url));
  }
}