import { Rol } from './usuario.model';

export const MAX_CARACTERES_COMENTARIO = 1000;

/** Fase 3: comentario dentro de un reporte (estudiante o administrador). */
export interface Comentario {
  id: number;
  reporteId: number;
  texto: string;
  autor: string;
  rol: Rol;
  /** true cuando lo escribio la persona que esta viendo el reporte (se muestra "(yo)"). */
  propio: boolean;
  fecha: string;
}