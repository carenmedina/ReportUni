import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CambioPassword, Perfil } from '../models/perfil.model';

@Injectable({ providedIn: 'root' })
export class PerfilService {
  private readonly base = `${environment.apiBaseUrl}/perfil`;

  readonly perfil = signal<Perfil | null>(null);
  readonly fotoUrl = signal<string | null>(null);

  constructor(private readonly http: HttpClient) {}

  
  cargar(): void {
    this.http.get<Perfil>(this.base).subscribe({
      next: (perfil) => {
        this.perfil.set(perfil);
        if (perfil.tieneFoto) {
          this.cargarFoto();
        } else {
          this.fijarFoto(null);
        }
      },
      error: () => {
        
      },
    });
  }

  obtener(): Observable<Perfil> {
    return this.http.get<Perfil>(this.base).pipe(tap((perfil) => this.perfil.set(perfil)));
  }

  subirFoto(archivo: File): Observable<Perfil> {
    const formData = new FormData();
    formData.append('foto', archivo, archivo.name);
    return this.http.post<Perfil>(`${this.base}/foto`, formData).pipe(
      tap((perfil) => {
        this.perfil.set(perfil);
        this.cargarFoto();
      }),
    );
  }

  quitarFoto(): Observable<Perfil> {
    return this.http.delete<Perfil>(`${this.base}/foto`).pipe(
      tap((perfil) => {
        this.perfil.set(perfil);
        this.fijarFoto(null);
      }),
    );
  }

  cambiarPassword(datos: CambioPassword): Observable<void> {
    return this.http.put<void>(`${this.base}/password`, datos);
  }

  limpiar(): void {
    this.perfil.set(null);
    this.fijarFoto(null);
  }

  interpretarError(error: HttpErrorResponse, accion: 'foto' | 'password'): string {
    switch (error.status) {
      case 400:
        return error.error?.mensaje ?? 'Los datos enviados no son válidos.';
      case 413:
        return 'La foto supera el tamaño máximo de 5 MB.';
      case 0:
        return 'No se pudo conectar con el servidor. Verifica que el backend esté en ejecución.';
      case 401:
        return 'Tu sesión expiró. Inicia sesión nuevamente.';
      default:
        return accion === 'foto'
          ? 'No pudimos actualizar tu foto. Intenta nuevamente.'
          : 'No pudimos cambiar tu contraseña. Intenta nuevamente.';
    }
  }

  private cargarFoto(): void {
    this.http.get(`${this.base}/foto`, { responseType: 'blob' }).subscribe({
      next: (blob) => this.fijarFoto(URL.createObjectURL(blob)),
      error: () => this.fijarFoto(null),
    });
  }

  private fijarFoto(url: string | null): void {
    const anterior = this.fotoUrl();
    if (anterior) {
      URL.revokeObjectURL(anterior);
    }
    this.fotoUrl.set(url);
  }
}