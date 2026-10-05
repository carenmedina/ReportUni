import { EstadoReporte, PrioridadReporte } from './reporte.model';

export type TipoDano =
  | 'FUGA_AGUA'
  | 'DANO_ELECTRICO'
  | 'AUDIOVISUAL'
  | 'TECHO_ESTRUCTURA'
  | 'MOBILIARIO'
  | 'ASEO'
  | 'OTRO';

export type Bloque = 'BLOQUE_A' | 'BLOQUE_B' | 'BLOQUE_C' | 'BIBLIOTECA' | 'ZONAS_COMUNES';

export interface OpcionCatalogo<T extends string> {
  valor: T;
  etiqueta: string;
}

export const TIPOS_DANO: OpcionCatalogo<TipoDano>[] = [
  { valor: 'FUGA_AGUA', etiqueta: 'Fuga de agua' },
  { valor: 'DANO_ELECTRICO', etiqueta: 'Daño eléctrico / iluminación' },
  { valor: 'AUDIOVISUAL', etiqueta: 'Videobeam / equipo audiovisual' },
  { valor: 'TECHO_ESTRUCTURA', etiqueta: 'Techo o estructura' },
  { valor: 'MOBILIARIO', etiqueta: 'Mobiliario dañado' },
  { valor: 'ASEO', etiqueta: 'Aseo / limpieza' },
  { valor: 'OTRO', etiqueta: 'Otro' },
];

export const PRIORIDADES_ESTIMADAS: OpcionCatalogo<PrioridadReporte>[] = [
  { valor: 'MEDIA', etiqueta: 'Media' },
  { valor: 'BAJA', etiqueta: 'Baja' },
  { valor: 'ALTA', etiqueta: 'Alta — afecta una actividad en curso' },
];

export const BLOQUES: OpcionCatalogo<Bloque>[] = [
  { valor: 'BLOQUE_A', etiqueta: 'Bloque A — Aulas' },
  { valor: 'BLOQUE_B', etiqueta: 'Bloque B — Laboratorios' },
  { valor: 'BLOQUE_C', etiqueta: 'Bloque C — Auditorio' },
  { valor: 'BIBLIOTECA', etiqueta: 'Biblioteca' },
  { valor: 'ZONAS_COMUNES', etiqueta: 'Zonas comunes / exteriores' },
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
  notificarPorCorreo: boolean;
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