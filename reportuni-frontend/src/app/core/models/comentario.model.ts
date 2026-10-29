import { Rol } from './usuario.model';

export const MAX_CARACTERES_COMENTARIO = 1000;

export interface Comentario {
  id: number;
  reporteId: number;
  texto: string;
  autor: string;
  rol: Rol;
  propio: boolean;
  fecha: string;
}