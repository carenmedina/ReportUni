import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AdminReporteItem, AdminResumen, FiltrosAdmin } from '../models/admin-reporte.model';
import { EstadoReporte, PrioridadReporte } from '../models/reporte.model';

@Injectable({ providedIn: 'root' })
export class AdminReporteService {
  private readonly base = `${environment.apiBaseUrl}/admin/reportes`;

  constructor(private readonly http: HttpClient) {}

  resumen(): Observable<AdminResumen> {
    return this.http.get<AdminResumen>(`${this.base}/resumen`);
  }

  listar(filtros: FiltrosAdmin): Observable<AdminReporteItem[]> {
    let params = new HttpParams();
    const texto = filtros.q.trim();

    if (texto) {
      params = params.set('q', texto);
    }
    if (filtros.estado !== 'TODOS') {
      params = params.set('estado', filtros.estado);
    }
    if (filtros.prioridad !== 'TODAS') {
      params = params.set('prioridad', filtros.prioridad);
    }
    if (filtros.tipoDano !== 'TODOS') {
      params = params.set('tipoDano', filtros.tipoDano);
    }
    if (filtros.bloque !== 'TODOS') {
      params = params.set('bloque', filtros.bloque);
    }

    return this.http.get<AdminReporteItem[]>(this.base, { params });
  }

  // Semana 7: gestion de estados (el backend responde con el reporte ya actualizado)
  cambiarEstado(id: number, estado: EstadoReporte): Observable<AdminReporteItem> {
    return this.http.put<AdminReporteItem>(`${this.base}/${id}/estado`, { estado });
  }

  cambiarPrioridad(id: number, prioridad: PrioridadReporte): Observable<AdminReporteItem> {
    return this.http.put<AdminReporteItem>(`${this.base}/${id}/prioridad`, { prioridad });
  }

  interpretarError(error: HttpErrorResponse): string {
    switch (error.status) {
      case 400:
        return error.error?.mensaje ?? 'El cambio no es válido.';
      case 404:
        return 'No encontramos ese reporte.';
      case 0:
        return 'No se pudo conectar con el servidor. Verifica que el backend esté en ejecución.';
      case 401:
        return 'Tu sesión expiró. Inicia sesión nuevamente.';
      case 403:
        return 'Solo el administrador puede ver esta información.';
      default:
        return 'Ocurrió un error inesperado. Intenta nuevamente en unos minutos.';
    }
  }
}