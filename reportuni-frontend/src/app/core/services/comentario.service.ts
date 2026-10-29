import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Comentario } from '../models/comentario.model';
import { Notificacion, TotalNoLeidas } from '../models/seguimiento.model';

@Injectable({ providedIn: 'root' })
export class ComentarioService {
  private readonly api = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient) {}

  listar(reporteId: number): Observable<Comentario[]> {
    return this.http.get<Comentario[]>(`${this.api}/reportes/${reporteId}/comentarios`);
  }

  comentar(reporteId: number, texto: string): Observable<Comentario> {
    return this.http.post<Comentario>(`${this.api}/reportes/${reporteId}/comentarios`, { texto });
  }

  // Avisos de comentarios nuevos (icono de comentarios del encabezado)
  avisos(): Observable<Notificacion[]> {
    return this.http.get<Notificacion[]>(`${this.api}/notificaciones/comentarios`);
  }

  totalNoLeidos(): Observable<TotalNoLeidas> {
    return this.http.get<TotalNoLeidas>(`${this.api}/notificaciones/comentarios/no-leidas`);
  }

  marcarTodosLeidos(): Observable<void> {
    return this.http.put<void>(`${this.api}/notificaciones/comentarios/leidas`, {});
  }

  interpretarError(error: HttpErrorResponse): string {
    const mensaje = (error.error as { mensaje?: string } | null)?.mensaje;
    switch (error.status) {
      case 0:
        return 'No se pudo conectar con el servidor. Verifica que el backend esté en ejecución.';
      case 400:
        return mensaje ?? 'El comentario no es válido.';
      case 401:
        return 'Tu sesión expiró. Inicia sesión nuevamente.';
      case 404:
        return 'No encontramos ese reporte.';
      default:
        return 'No pudimos enviar el comentario. Intenta nuevamente en unos minutos.';
    }
  }
}