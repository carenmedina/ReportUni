export interface ConteoEstadistica {
  codigo: string;
  total: number;
}

export interface Estadisticas {
  totalReportes: number;
  porTipo: ConteoEstadistica[];
  porBloque: ConteoEstadistica[];
  porPrioridad: ConteoEstadistica[];
  promedioResolucionDias: number | null;
  reportesResueltosMedidos: number;
}

export interface RangoFechas {
  desde: string; // YYYY-MM-DD o vacio
  hasta: string; // YYYY-MM-DD o vacio
}