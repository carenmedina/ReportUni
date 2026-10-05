import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ErrorApi, NuevoReporteDatos, ReporteCreado } from '../models/nuevo-reporte.models';

export interface ErrorInterpretado {
  mensaje: string;
  errores: Record<string, string>;
}

@Injectable({ providedIn: 'root' })
export class ReporteService {
  constructor(private readonly http: HttpClient) {}

  crear(datos: NuevoReporteDatos, fotos: File[]): Observable<ReporteCreado> {
    const formData = new FormData();
    formData.append('tipoDano', datos.tipoDano);
    formData.append('prioridadEstimada', datos.prioridadEstimada);
    formData.append('bloque', datos.bloque);
    formData.append('espacioEspecifico', datos.espacioEspecifico);
    formData.append('descripcion', datos.descripcion);
    formData.append('notificarPorCorreo', String(datos.notificarPorCorreo));

    if (datos.latitud !== null && datos.longitud !== null) {
      formData.append('latitud', String(datos.latitud));
      formData.append('longitud', String(datos.longitud));
    }

    fotos.forEach((foto) => formData.append('fotos', foto, foto.name));

    return this.http.post<ReporteCreado>(`${environment.apiBaseUrl}/reportes`, formData);
  }

  interpretarError(error: HttpErrorResponse): ErrorInterpretado {
    const cuerpo: ErrorApi =
      typeof error.error === 'object' && error.error !== null ? (error.error as ErrorApi) : {};
    const errores = cuerpo.errores ?? {};

    switch (error.status) {
      case 0:
        return {
          mensaje:
            'No se pudo conectar con el servidor. Verifica tu conexión y que el backend esté en ejecución.',
          errores,
        };
      case 400:
        return {
          mensaje: cuerpo.mensaje ?? 'Revisa los datos del formulario e intenta nuevamente.',
          errores,
        };
      case 401:
        return { mensaje: 'Tu sesión expiró. Inicia sesión nuevamente.', errores };
      case 403:
        return { mensaje: 'Tu usuario no tiene permiso para crear reportes.', errores };
      case 413:
        return {
          mensaje: 'Las fotografías superan el tamaño permitido (máximo 10 MB por foto).',
          errores,
        };
      default:
        return {
          mensaje:
            cuerpo.mensaje ??
            'Ocurrió un error inesperado al enviar el reporte. Intenta nuevamente en unos minutos.',
          errores,
        };
    }
  }
}