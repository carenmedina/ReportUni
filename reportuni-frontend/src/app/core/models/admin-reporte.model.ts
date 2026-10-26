import { EstadoReporte, PrioridadReporte } from './reporte.model';

export interface AdminReporteItem {
  id: number;
  codigo: string;
  tipoDano: string;
  bloque: string;
  espacioEspecifico: string;
  prioridadEstimada: PrioridadReporte;
  estado: EstadoReporte;
  reportadoPor: string;
  reportadoPorCorreo: string;
  fechaCreacion: string;
}

export interface AdminResumen {
  total: number;
  pendientes: number;
  enProceso: number;
  resueltos: number;
  altaPrioridadPendientes: number;
  recientes: AdminReporteItem[];
}

export const OPCIONES_ESTADO: { valor: EstadoReporte; etiqueta: string }[] = [
  { valor: 'PENDIENTE', etiqueta: 'Pendiente' },
  { valor: 'EN_PROCESO', etiqueta: 'En proceso' },
  { valor: 'RESUELTO', etiqueta: 'Resuelto' },
];

export const OPCIONES_PRIORIDAD: { valor: PrioridadReporte; etiqueta: string }[] = [
  { valor: 'ALTA', etiqueta: 'Alta' },
  { valor: 'MEDIA', etiqueta: 'Media' },
  { valor: 'BAJA', etiqueta: 'Baja' },
];

export interface FiltrosAdmin {
  q: string;
  estado: string; // 'TODOS' o un EstadoReporte
  prioridad: string; // 'TODAS' o una PrioridadReporte
  tipoDano: string; // 'TODOS' o un TipoDano
  bloque: string; // 'TODOS' o un Bloque
}

export const FILTROS_VACIOS: FiltrosAdmin = {
  q: '',
  estado: 'TODOS',
  prioridad: 'TODAS',
  tipoDano: 'TODOS',
  bloque: 'TODOS',
};

export const OPCIONES_TIPO_DANO: { valor: string; etiqueta: string }[] = [
  { valor: 'FUGA_AGUA', etiqueta: 'Fuga de agua' },
  { valor: 'DANO_ELECTRICO', etiqueta: 'Daño eléctrico' },
  { valor: 'AUDIOVISUAL', etiqueta: 'Equipo audiovisual' },
  { valor: 'TECHO_ESTRUCTURA', etiqueta: 'Techo o estructura' },
  { valor: 'MOBILIARIO', etiqueta: 'Mobiliario' },
  { valor: 'ASEO', etiqueta: 'Aseo' },
  { valor: 'OTRO', etiqueta: 'Otro' },
];

export const OPCIONES_BLOQUE: { valor: string; etiqueta: string }[] = [
  { valor: 'BLOQUE_A', etiqueta: 'Bloque A' },
  { valor: 'BLOQUE_B', etiqueta: 'Bloque B' },
  { valor: 'BLOQUE_C', etiqueta: 'Bloque C' },
  { valor: 'BIBLIOTECA', etiqueta: 'Biblioteca' },
  { valor: 'ZONAS_COMUNES', etiqueta: 'Zonas comunes' },
];