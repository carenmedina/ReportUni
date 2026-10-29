import { EstadoReporte, PrioridadReporte } from './reporte.model';

export type TipoDano =
  | 'FUGA_AGUA'
  | 'DANO_ELECTRICO'
  | 'AUDIOVISUAL'
  | 'ESTRUCTURA'
  | 'ASEO'
  | 'OTRO';

export type Bloque = 'BLOQUE_1' | 'BLOQUE_2' | 'BIBLIOTECA' | 'ZONAS_COMUNES';

export interface OpcionCatalogo<T extends string> {
  valor: T;
  etiqueta: string;
}

export const TIPOS_DANO: OpcionCatalogo<TipoDano>[] = [
  { valor: 'FUGA_AGUA', etiqueta: 'Fuga de agua' },
  { valor: 'DANO_ELECTRICO', etiqueta: 'Daño eléctrico' },
  { valor: 'AUDIOVISUAL', etiqueta: 'Equipo audiovisual' },
  { valor: 'ESTRUCTURA', etiqueta: 'Estructura' },
  { valor: 'ASEO', etiqueta: 'Aseo - Limpieza' },
  { valor: 'OTRO', etiqueta: 'Otros' },
];

export const PRIORIDADES_ESTIMADAS: OpcionCatalogo<PrioridadReporte>[] = [
  { valor: 'BAJA', etiqueta: 'Baja' },
  { valor: 'MEDIA', etiqueta: 'Media' },
  { valor: 'ALTA', etiqueta: 'Alta' },
];

export const BLOQUES: OpcionCatalogo<Bloque>[] = [
  { valor: 'BLOQUE_1', etiqueta: 'Bloque 1' },
  { valor: 'BLOQUE_2', etiqueta: 'Bloque 2' },
  { valor: 'BIBLIOTECA', etiqueta: 'Biblioteca' },
  { valor: 'ZONAS_COMUNES', etiqueta: 'Zonas comunes - Exteriores' },
];

export const LIMITE_FOTOS = 5;
export const MAX_BYTES_FOTO = 10 * 1024 * 1024;
export const TIPOS_FOTO_PERMITIDOS = ['image/jpeg', 'image/png'];

export interface NuevoReporteDatos {
  tipoDano: TipoDano;
  prioridadEstimada: PrioridadReporte;
  bloque: Bloque;
  espacioEspecifico: string;
  descripcion: string;
  latitud: number | null;
  longitud: number | null;
}

export interface ReporteCreado {
  id: number;
  codigo: string;
  estado: EstadoReporte;
  fechaCreacion: string;
  totalFotos: number;
}

export interface ErrorApi {
  mensaje?: string;
  errores?: Record<string, string>;
}