export type TipoCambio = 'ESTADO' | 'PRIORIDAD';

export interface HistorialItem {
  id: number;
  tipo: TipoCambio;
  valorAnterior: string;
  valorNuevo: string;
  realizadoPor: string;
  fecha: string;
}

export interface Notificacion {
  id: number;
  reporteId: number;
  codigo: string;
  mensaje: string;
  leida: boolean;
  fechaCreacion: string;
}

export interface TotalNoLeidas {
  total: number;
}

const ETIQUETAS: Record<string, string> = {
  PENDIENTE: 'Pendiente',
  EN_PROCESO: 'En proceso',
  RESUELTO: 'Resuelto',
  ALTA: 'Alta',
  MEDIA: 'Media',
  BAJA: 'Baja',
};

/** Convierte PENDIENTE, EN_PROCESO, ALTA... en texto legible. */
export function etiquetaValor(valor: string): string {
  return ETIQUETAS[valor] ?? valor;
}

export function descripcionCambio(item: HistorialItem): string {
  const campo = item.tipo === 'ESTADO' ? 'Estado' : 'Prioridad';
  return `${campo}: ${etiquetaValor(item.valorAnterior)} → ${etiquetaValor(item.valorNuevo)}`;
}