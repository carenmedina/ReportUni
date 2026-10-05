import { Reporte } from './reporte.model';

export const REPORTES_EJEMPLO: Reporte[] = [
  { id: '0231', tipo: 'Fuga de agua', ubicacion: 'Bloque A · Salón 204', fecha: '14 sept.', estado: 'RESUELTO', prioridad: 'ALTA' },
  { id: '0230', tipo: 'Videobeam dañado', ubicacion: 'Bloque C · Auditorio', fecha: '16 sept.', estado: 'EN_PROCESO', prioridad: 'MEDIA' },
  { id: '0229', tipo: 'Techo con filtración', ubicacion: 'Bloque B · Pasillo 2do piso', fecha: '17 sept.', estado: 'PENDIENTE', prioridad: 'ALTA' },
  { id: '0228', tipo: 'Silla dañada', ubicacion: 'Biblioteca · Sala de estudio', fecha: '12 sept.', estado: 'RESUELTO', prioridad: 'BAJA' },
  { id: '0224', tipo: 'Toma eléctrica dañada', ubicacion: 'Bloque A · Salón 108', fecha: '05 sept.', estado: 'RESUELTO', prioridad: 'ALTA' },
  { id: '0219', tipo: 'Fuga en baños', ubicacion: 'Bloque B · Baños 1er piso', fecha: '28 ago.', estado: 'RESUELTO', prioridad: 'MEDIA' },
];
