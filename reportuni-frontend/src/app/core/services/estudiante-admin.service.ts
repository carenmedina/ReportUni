import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CargaEstudiantesResultado } from '../models/carga-estudiantes.model';

@Injectable({ providedIn: 'root' })
export class EstudianteAdminService {
  private readonly base = `${environment.apiBaseUrl}/admin/estudiantes`;

  constructor(private readonly http: HttpClient) {}

  cargar(archivo: File): Observable<CargaEstudiantesResultado> {
    const formData = new FormData();
    formData.append('archivo', archivo, archivo.name);
    return this.http.post<CargaEstudiantesResultado>(`${this.base}/carga`, formData);
  }

  interpretarError(error: HttpErrorResponse): string {
    switch (error.status) {
      case 400:
        return error.error?.mensaje ?? 'El archivo no es válido.';
      case 413:
        return 'El archivo supera el tamaño máximo de 2 MB.';
      case 0:
        return 'No se pudo conectar con el servidor. Verifica que el backend esté en ejecución.';
      case 401:
        return 'Tu sesión expiró. Inicia sesión nuevamente.';
      case 403:
        return 'Solo el administrador puede cargar estudiantes.';
      default:
        return 'Ocurrió un error inesperado. Intenta nuevamente en unos minutos.';
    }
  }
}