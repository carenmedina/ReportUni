export type Rol = 'ESTUDIANTE' | 'ADMINISTRADOR';

export interface LoginRequest {
  correoInstitucional: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  usuarioId: number;
  nombreCompleto: string;
  correoInstitucional: string;
  rol: Rol;
}

export interface ErrorResponse {
  mensaje: string;
}

export interface UsuarioSesion {
  usuarioId: number;
  nombreCompleto: string;
  correoInstitucional: string;
  rol: Rol;
}
