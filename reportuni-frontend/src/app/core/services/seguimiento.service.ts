import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { HistorialItem, Notificacion, TotalNoLeidas } from '../models/seguimiento.model';

@Injectable({ providedIn: 'root' })
export class SeguimientoService {
  private readonly api = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient) {}

  historial(reporteId: number): Observable<HistorialItem[]> {
    return this.http.get<HistorialItem[]>(`${this.api}/reportes/${reporteId}/historial`);
  }

  notificaciones(): Observable<Notificacion[]> {
    return this.http.get<Notificacion[]>(`${this.api}/notificaciones`);
  }

  totalNoLeidas(): Observable<TotalNoLeidas> {
    return this.http.get<TotalNoLeidas>(`${this.api}/notificaciones/no-leidas`);
  }

  marcarLeida(id: number): Observable<void> {
    return this.http.put<void>(`${this.api}/notificaciones/${id}/leida`, {});
  }

  marcarTodasLeidas(): Observable<void> {
    return this.http.put<void>(`${this.api}/notificaciones/leidas`, {});
  }
}