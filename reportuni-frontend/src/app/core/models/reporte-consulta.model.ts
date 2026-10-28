import { EstadoReporte, PrioridadReporte } from './reporte.model';

export interface ReporteResumen {
  id: number;
  codigo: string;
  tipoDano: string;
  bloque: string;
  espacioEspecifico: string;
  prioridadEstimada: PrioridadReporte;
  estado: EstadoReporte;
  fechaCreacion: string;
  totalFotos: number;
}

export interface FotoInfo {
  id: number;
  nombreOriginal: string;
  tipoContenido: string;
  tamanoBytes: number;
  orden: number;
}

export interface ReporteDetalle {
  id: number;
  codigo: string;
  tipoDano: string;
  bloque: string;
  espacioEspecifico: string;
  prioridadEstimada: PrioridadReporte;
  estado: EstadoReporte;
  descripcion: string;
  latitud: number | null;
  longitud: number | null;
  reportadoPor: string;
  fechaCreacion: string;
  asignadoA: string | null;
  ultimaActualizacion: string;
  fotos: FotoInfo[];
}

const TIPOS_DANO: Record<string, string> = {
  FUGA_AGUA: 'Fuga de agua',
  DANO_ELECTRICO: 'Daño eléctrico',
  AUDIOVISUAL: 'Equipo audiovisual',
  TECHO_ESTRUCTURA: 'Techo o estructura',
  MOBILIARIO: 'Mobiliario',
  ASEO: 'Aseo',
  OTRO: 'Otro',
};

const BLOQUES: Record<string, string> = {
  BLOQUE_A: 'Bloque A',
  BLOQUE_B: 'Bloque B',
  BLOQUE_C: 'Bloque C',
  BIBLIOTECA: 'Biblioteca',
  ZONAS_COMUNES: 'Zonas comunes',
};

export function etiquetaTipoDano(codigo: string): string {
  return TIPOS_DANO[codigo] ?? codigo;
}

export function etiquetaBloque(codigo: string): string {
  return BLOQUES[codigo] ?? codigo;
}

export function ubicacionTexto(bloque: string, espacio: string): string {
  return `${etiquetaBloque(bloque)} · ${espacio}`;
}

export function etiquetaEstado(estado: EstadoReporte): string {
  switch (estado) {
    case 'RESUELTO':
      return 'Resuelto';
    case 'EN_PROCESO':
      return 'En proceso';
    default:
      return 'Pendiente';
  }
}

export function etiquetaPrioridad(prioridad: PrioridadReporte): string {
  return prioridad === 'ALTA' ? 'Alta' : prioridad === 'MEDIA' ? 'Media' : 'Baja';
}

export function formatearFecha(iso: string, conHora = false): string {
  const fecha = new Date(iso);
  if (Number.isNaN(fecha.getTime())) {
    return iso;
  }
  return fecha.toLocaleString(
    'es-CO',
    conHora
      ? { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }
      : { day: '2-digit', month: 'short' },
  );
}