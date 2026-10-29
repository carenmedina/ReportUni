import { Component, ElementRef, HostListener, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { SeguimientoService } from '../../core/services/seguimiento.service';
import { AuthService } from '../../core/services/auth.service';
import { Notificacion, TotalNoLeidas } from '../../core/models/seguimiento.model';
import { formatearFecha } from '../../core/models/reporte-consulta.model';

/** Semana 8: campana del encabezado con las notificaciones del usuario. */
@Component({
  selector: 'app-notificaciones-campana',
  standalone: true,
  templateUrl: './notificaciones-campana.html',
  styleUrl: './notificaciones-campana.scss',
})
export class NotificacionesCampana implements OnInit, OnDestroy {
  private readonly servicio = inject(SeguimientoService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly host = inject(ElementRef<HTMLElement>);

  /** Cada cuanto se consulta si hay avisos nuevos (ms). */
  static readonly INTERVALO_MS = 30000;

  readonly abierto = signal(false);
  readonly noLeidas = signal(0);
  readonly lista = signal<Notificacion[]>([]);
  readonly cargando = signal(false);
  readonly error = signal(false);

  private temporizador: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.consultarTotal();
    this.temporizador = setInterval(() => this.consultarTotal(), NotificacionesCampana.INTERVALO_MS);
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

  abrirNotificacion(n: Notificacion): void {
    this.abierto.set(false);
    if (!n.leida) {
      this.servicio.marcarLeida(n.id).subscribe({
        next: () => {
          this.lista.update((l) => l.map((x) => (x.id === n.id ? { ...x, leida: true } : x)));
          this.noLeidas.update((t) => Math.max(0, t - 1));
        },
        error: () => undefined,
      });
    }
    // El administrador gestiona el reporte desde su propia pantalla
    const esAdmin = this.auth.usuarioActual()?.rol === 'ADMINISTRADOR';
    this.router.navigate([esAdmin ? '/admin/reportes' : '/mis-reportes', n.reporteId]);
  }

  marcarTodas(): void {
    this.servicio.marcarTodasLeidas().subscribe({
      next: () => {
        this.lista.update((l) => l.map((x) => ({ ...x, leida: true })));
        this.noLeidas.set(0);
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
    this.servicio.totalNoLeidas().subscribe({
      next: (r: TotalNoLeidas) => this.noLeidas.set(r.total),
      error: () => undefined,
    });
  }

  private cargarLista(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.servicio.notificaciones().subscribe({
      next: (lista) => {
        this.lista.set(lista);
        this.noLeidas.set(lista.filter((n) => !n.leida).length);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      },
    });
  }
}