export interface ErrorFilaCarga {
  fila: number;
  correo: string;
  motivo: string;
}

export interface CargaEstudiantesResultado {
  filasLeidas: number;
  creados: number;
  actualizados: number;
  rechazados: number;
  errores: ErrorFilaCarga[];
}

export const MAX_BYTES_CSV = 2 * 1024 * 1024;

export const PLANTILLA_ESTUDIANTES =
  'documento;nombre;correo\n' +
  '1075000001;Nombre Apellido;nombre.apellido@fet.edu.co\n';