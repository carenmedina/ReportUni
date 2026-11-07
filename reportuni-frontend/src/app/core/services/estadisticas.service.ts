import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Estadisticas, RangoFechas } from '../models/estadisticas.model';

@Injectable({ providedIn: 'root' })
export class EstadisticasService {
  private readonly base = `${environment.apiBaseUrl}/admin/estadisticas`;

  constructor(private readonly http: HttpClient) {}

  obtener(rango: RangoFechas): Observable<Estadisticas> {
    let params = new HttpParams();
    if (rango.desde) {
      params = params.set('desde', rango.desde);
    }
    if (rango.hasta) {
      params = params.set('hasta', rango.hasta);
    }
    return this.http.get<Estadisticas>(this.base, { params });
  }

  interpretarError(error: HttpErrorResponse): string {
    switch (error.status) {
      case 400:
        return error.error?.mensaje ?? 'El rango de fechas no es válido.';
      case 0:
        return 'No se pudo conectar con el servidor. Verifica que el backend esté en ejecución.';
      case 401:
        return 'Tu sesión expiró. Inicia sesión nuevamente.';
      case 403:
        return 'Solo el administrador puede ver las estadísticas.';
      default:
        return 'No pudimos cargar las estadísticas. Intenta nuevamente en unos minutos.';
    }
  }
}