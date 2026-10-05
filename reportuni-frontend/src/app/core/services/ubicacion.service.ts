import { Injectable } from '@angular/core';

export interface UbicacionGps {
  latitud: number;
  longitud: number;
  precisionMetros: number;
}

@Injectable({ providedIn: 'root' })
export class UbicacionService {
  obtenerActual(): Promise<UbicacionGps> {
    return new Promise((resolve, reject) => {
      if (typeof navigator === 'undefined' || !('geolocation' in navigator)) {
        reject(new Error('Tu navegador no permite obtener la ubicación.'));
        return;
      }

      navigator.geolocation.getCurrentPosition(
        (posicion) =>
          resolve({
            latitud: this.redondear(posicion.coords.latitude),
            longitud: this.redondear(posicion.coords.longitude),
            precisionMetros: Math.round(posicion.coords.accuracy),
          }),
        (error) => reject(new Error(this.mensajePorCodigo(error.code))),
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 },
      );
    });
  }

  private redondear(valor: number): number {
    return Math.round(valor * 1_000_000) / 1_000_000;
  }

  private mensajePorCodigo(codigo: number): string {
    switch (codigo) {
      case 1:
        return 'No diste permiso para usar tu ubicación. Puedes continuar indicando el bloque y el espacio específico.';
      case 3:
        return 'Se agotó el tiempo para obtener tu ubicación. Intenta de nuevo.';
      default:
        return 'No se pudo determinar tu ubicación en este momento.';
    }
  }
}