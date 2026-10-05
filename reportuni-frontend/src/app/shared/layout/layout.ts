import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { NavegacionService } from '../../core/services/navegacion.service';
import { MenuItem } from '../../core/models/navegacion.model';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
})
export class Layout implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly navegacionService = inject(NavegacionService);

  readonly menuMovilAbierto = signal(false);
  readonly itemsMenu = signal<MenuItem[]>([]);

  readonly usuario = this.authService.usuarioActual;

  ngOnInit(): void {
    this.navegacionService.obtenerMenu().subscribe({
      next: (respuesta) => this.itemsMenu.set(respuesta.items),
      error: () => this.itemsMenu.set([]),
    });
  }

  alternarMenuMovil(): void {
    this.menuMovilAbierto.update((valor) => !valor);
  }

  cerrarMenuMovil(): void {
    this.menuMovilAbierto.set(false);
  }

  cerrarSesion(): void {
    this.cerrarMenuMovil();
    this.authService.logout();
  }

  iniciales(nombre: string | undefined): string {
    if (!nombre) {
      return '';
    }
    return nombre
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((parte) => parte[0]?.toUpperCase())
      .join('');
  }
}
