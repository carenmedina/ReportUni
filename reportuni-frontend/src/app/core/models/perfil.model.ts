import { Rol } from './usuario.model';

export interface ResumenPerfil {
  total: number | null;
  pendientes: number | null;
  enProceso: number | null;
  resueltos: number | null;
  gestionados: number | null;
}

export interface Perfil {
  nombreCompleto: string;
  correoInstitucional: string;
  rol: Rol;
  tieneFoto: boolean;
  resumen: ResumenPerfil;
}

export interface CambioPassword {
  actual: string;
  nueva: string;
  confirmacion: string;
}

export const MAX_BYTES_FOTO_PERFIL = 5 * 1024 * 1024;
export const TIPOS_FOTO_PERFIL = ['image/jpeg', 'image/png'];
export const MIN_PASSWORD = 8;
export const MAX_PASSWORD = 64;