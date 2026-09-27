export type EstadoReporte = 'PENDIENTE' | 'EN_PROCESO' | 'RESUELTO';
export type PrioridadReporte = 'ALTA' | 'MEDIA' | 'BAJA';

export interface Reporte {
  id: string;
  tipo: string;
  ubicacion: string;
  fecha: string;
  estado: EstadoReporte;
  prioridad: PrioridadReporte;
}
