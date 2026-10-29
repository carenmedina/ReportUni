export type TipoCambio = 'ESTADO' | 'PRIORIDAD' | 'ASIGNACION' | 'ACTUALIZACION';

export interface FotoHistorial {
  id: number;
  nombreOriginal: string;
  tipoContenido: string;
  tamanoBytes: number;
}

export interface HistorialItem {
  id: number;
  tipo: TipoCambio;
  valorAnterior: string;
  valorNuevo: string;
  nota: string | null;
  realizadoPor: string;
  fecha: string;
  fotos: FotoHistorial[];
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

export const OPCIONES_AREA: { valor: string; etiqueta: string }[] = [
  { valor: 'SOPORTE_TECNICO', etiqueta: 'Soporte Técnico · Infraestructura' },
  { valor: 'MANTENIMIENTO', etiqueta: 'Mantenimiento general' },
  { valor: 'ELECTRICO', etiqueta: 'Mantenimiento eléctrico' },
  { valor: 'AUDIOVISUALES', etiqueta: 'Audiovisuales' },
  { valor: 'SERVICIOS_GENERALES', etiqueta: 'Servicios generales · Aseo' },
];

const ETIQUETAS: Record<string, string> = {
  PENDIENTE: 'Pendiente',
  EN_PROCESO: 'En proceso',
  RESUELTO: 'Resuelto',
  ALTA: 'Alta',
  MEDIA: 'Media',
  BAJA: 'Baja',
};

export function etiquetaValor(valor: string): string {
  return ETIQUETAS[valor] ?? OPCIONES_AREA.find((a) => a.valor === valor)?.etiqueta ?? valor;
}

export function etiquetaArea(valor: string | null | undefined): string {
  return valor ? etiquetaValor(valor) : 'Sin asignar';
}

export function descripcionCambio(item: HistorialItem): string {
  switch (item.tipo) {
    case 'ESTADO':
      return `Estado: ${etiquetaValor(item.valorAnterior)} → ${etiquetaValor(item.valorNuevo)}`;
    case 'PRIORIDAD':
      return `Prioridad: ${etiquetaValor(item.valorAnterior)} → ${etiquetaValor(item.valorNuevo)}`;
    case 'ASIGNACION':
      return item.valorAnterior
        ? `Reasignado: ${etiquetaValor(item.valorAnterior)} → ${etiquetaValor(item.valorNuevo)}`
        : `Asignado a ${etiquetaValor(item.valorNuevo)}`;
    default:
      return 'Actualización del proceso';
  }
}