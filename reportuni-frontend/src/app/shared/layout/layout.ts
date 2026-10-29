import { Component, HostListener, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { NavegacionService } from '../../core/services/navegacion.service';
import { MenuItem } from '../../core/models/navegacion.model';
import { NotificacionesCampana } from '../notificaciones-campana/notificaciones-campana';
import { ComentariosCampana } from '../comentarios-campana/comentarios-campana';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet, NotificacionesCampana, ComentariosCampana],
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
})
export class Layout implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly navegacionService = inject(NavegacionService);

  readonly menuUsuarioAbierto = signal(false);
  readonly itemsMenu = signal<MenuItem[]>([]);

  readonly usuario = this.authService.usuarioActual;

  ngOnInit(): void {
    this.navegacionService.obtenerMenu().subscribe({
      next: (respuesta) => this.itemsMenu.set(respuesta.items),
      error: () => this.itemsMenu.set([]),
    });
  }

  alternarMenuUsuario(): void {
    this.menuUsuarioAbierto.update((valor) => !valor);
  }

  @HostListener('document:click', ['$event'])
  cerrarSiClicFuera(evento: Event): void {
    const destino = evento.target as HTMLElement | null;
    if (this.menuUsuarioAbierto() && !destino?.closest('.ru-usermenu')) {
      this.menuUsuarioAbierto.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    this.menuUsuarioAbierto.set(false);
  }

  etiquetaRol(): string {
    return this.usuario()?.rol === 'ADMINISTRADOR' ? 'Administrador' : 'Estudiante';
  }

  cerrarSesion(): void {
    this.menuUsuarioAbierto.set(false);
    this.authService.logout();
  }
}