import { Component, ElementRef, HostListener, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ComentarioService } from '../../core/services/comentario.service';
import { SeguimientoService } from '../../core/services/seguimiento.service';
import { AuthService } from '../../core/services/auth.service';
import { Notificacion } from '../../core/models/seguimiento.model';
import { formatearFecha } from '../../core/models/reporte-consulta.model';

/**
 * Fase 3: icono de comentarios del encabezado. Reune los avisos de comentarios hechos en cualquiera
 * de los reportes de la persona; al elegir uno, se abre ese reporte, donde se ve toda la conversacion.
 */
@Component({
  selector: 'app-comentarios-campana',
  standalone: true,
  templateUrl: './comentarios-campana.html',
  // Mismo aspecto que la campana de notificaciones
  styleUrl: '../notificaciones-campana/notificaciones-campana.scss',
})
export class ComentariosCampana implements OnInit, OnDestroy {
  private readonly comentarios = inject(ComentarioService);
  private readonly seguimiento = inject(SeguimientoService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly host = inject(ElementRef<HTMLElement>);

  /** Cada cuanto se consulta si hay comentarios nuevos (ms). */
  static readonly INTERVALO_MS = 30000;

  readonly abierto = signal(false);
  readonly noLeidos = signal(0);
  readonly lista = signal<Notificacion[]>([]);
  readonly cargando = signal(false);
  readonly error = signal(false);

  private temporizador: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.consultarTotal();
    this.temporizador = setInterval(() => this.consultarTotal(), ComentariosCampana.INTERVALO_MS);
  }

  ngOnDestroy(): void {
    if (this.temporizador) {
      clearInterval(this.temporizador);
    }
  }

  alternar(): void {
    const abrir = !this.abierto();
    this.abierto.set(abrir);
    if (abrir) {
      this.cargarLista();
    }
  }

  @HostListener('document:click', ['$event'])
  cerrarSiClickFuera(evento: Event): void {
    if (this.abierto() && !this.host.nativeElement.contains(evento.target as Node)) {
      this.abierto.set(false);
    }
  }

  abrirComentario(n: Notificacion): void {
    this.abierto.set(false);
    if (!n.leida) {
      this.seguimiento.marcarLeida(n.id).subscribe({
        next: () => {
          this.lista.update((l) => l.map((x) => (x.id === n.id ? { ...x, leida: true } : x)));
          this.noLeidos.update((t) => Math.max(0, t - 1));
        },
        error: () => undefined,
      });
    }
    // El administrador gestiona el reporte desde su propia pantalla
    const esAdmin = this.auth.usuarioActual()?.rol === 'ADMINISTRADOR';
    this.router.navigate([esAdmin ? '/admin/reportes' : '/mis-reportes', n.reporteId]);
  }

  marcarTodos(): void {
    this.comentarios.marcarTodosLeidos().subscribe({
      next: () => {
        this.lista.update((l) => l.map((x) => ({ ...x, leida: true })));
        this.noLeidos.set(0);
      },
      error: () => undefined,
    });
  }

  fecha(iso: string): string {
    return formatearFecha(iso, true);
  }

  private consultarTotal(): void {
    if (!this.auth.usuarioActual()) {
      return;
    }
    this.comentarios.totalNoLeidos().subscribe({
      next: (r) => this.noLeidos.set(r.total),
      error: () => undefined,
    });
  }

  private cargarLista(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.comentarios.avisos().subscribe({
      next: (lista) => {
        this.lista.set(lista);
        this.noLeidos.set(lista.filter((n) => !n.leida).length);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      },
    });
  }
}