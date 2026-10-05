import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { rutaPorDefecto } from './role.guard';

@Component({
  selector: 'app-entrada-redirect',
  standalone: true,
  template: '',
})
export class EntradaRedirect implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    const usuario = this.authService.usuarioActual();
    this.router.navigateByUrl(rutaPorDefecto(usuario?.rol), { replaceUrl: true });
  }
}