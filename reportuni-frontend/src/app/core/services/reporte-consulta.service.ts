import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ReporteDetalle, ReporteResumen } from '../models/reporte-consulta.model';

@Injectable({ providedIn: 'root' })
export class ReporteConsultaService {
  private readonly base = `${environment.apiBaseUrl}/reportes`;

  constructor(private readonly http: HttpClient) {}

  listarMios(): Observable<ReporteResumen[]> {
    return this.http.get<ReporteResumen[]>(`${this.base}/mis`);
  }

  obtenerDetalle(id: number): Observable<ReporteDetalle> {
    return this.http.get<ReporteDetalle>(`${this.base}/${id}`);
  }

  obtenerFoto(reporteId: number, fotoId: number): Observable<Blob> {
    return this.http.get(`${this.base}/${reporteId}/fotos/${fotoId}`, { responseType: 'blob' });
  }

  interpretarError(error: HttpErrorResponse): string {
    switch (error.status) {
      case 0:
        return 'No se pudo conectar con el servidor. Verifica que el backend esté en ejecución.';
      case 401:
        return 'Tu sesión expiró. Inicia sesión nuevamente.';
      case 403:
        return 'No tienes permiso para ver esta información.';
      case 404:
        return 'No encontramos ese reporte.';
      default:
        return 'Ocurrió un error inesperado. Intenta nuevamente en unos minutos.';
    }
  }
}