import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse, UsuarioSesion } from '../models/usuario.model';

const TOKEN_KEY = 'reportuni_token';
const USUARIO_KEY = 'reportuni_usuario';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly usuarioActualSignal = signal<UsuarioSesion | null>(this.leerUsuarioGuardado());

  readonly usuarioActual = this.usuarioActualSignal.asReadonly();

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
  ) {}

  login(credenciales: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiBaseUrl}/auth/login`, credenciales)
      .pipe(
        tap((respuesta) => {
          const usuario: UsuarioSesion = {
            usuarioId: respuesta.usuarioId,
            nombreCompleto: respuesta.nombreCompleto,
            correoInstitucional: respuesta.correoInstitucional,
            rol: respuesta.rol,
          };
          localStorage.setItem(TOKEN_KEY, respuesta.token);
          localStorage.setItem(USUARIO_KEY, JSON.stringify(usuario));
          this.usuarioActualSignal.set(usuario);
        }),
      );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USUARIO_KEY);
    this.usuarioActualSignal.set(null);
    this.router.navigate(['/login']);
  }

  obtenerToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  estaAutenticado(): boolean {
    return !!this.obtenerToken();
  }

  esAdministrador(): boolean {
    return this.usuarioActualSignal()?.rol === 'ADMINISTRADOR';
  }

  private leerUsuarioGuardado(): UsuarioSesion | null {
    const crudo = localStorage.getItem(USUARIO_KEY);
    if (!crudo) {
      return null;
    }
    try {
      return JSON.parse(crudo) as UsuarioSesion;
    } catch {
      return null;
    }
  }
}
