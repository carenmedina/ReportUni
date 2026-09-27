export interface MenuItem {
  etiqueta: string;
  ruta: string;
  icono: string;
}

export interface NavegacionResponse {
  rol: string;
  items: MenuItem[];
}
