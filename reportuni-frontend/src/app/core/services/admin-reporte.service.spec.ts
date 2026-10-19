import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminReporteService } from './admin-reporte.service';
import { FILTROS_VACIOS } from '../models/admin-reporte.model';

describe('AdminReporteService', () => {
  let servicio: AdminReporteService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(AdminReporteService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pide el resumen al endpoint de administrador', () => {
    servicio.resumen().subscribe();
    const req = http.expectOne((r) => r.url.endsWith('/admin/reportes/resumen'));
    expect(req.request.method).toBe('GET');
    req.flush({ total: 0, pendientes: 0, enProceso: 0, resueltos: 0, altaPrioridadPendientes: 0, recientes: [] });
  });

  it('no envia parametros cuando los filtros estan vacios', () => {
    servicio.listar(FILTROS_VACIOS).subscribe();
    const req = http.expectOne((r) => r.url.endsWith('/admin/reportes'));
    expect(req.request.params.keys().length).toBe(0);
    req.flush([]);
  });

  it('envia solo los filtros que tienen valor', () => {
    servicio.listar({ ...FILTROS_VACIOS, q: ' salon ', estado: 'PENDIENTE', prioridad: 'ALTA' }).subscribe();
    const req = http.expectOne((r) => r.url.endsWith('/admin/reportes'));
    expect(req.request.params.get('q')).toBe('salon');
    expect(req.request.params.get('estado')).toBe('PENDIENTE');
    expect(req.request.params.get('prioridad')).toBe('ALTA');
    expect(req.request.params.has('bloque')).toBeFalse();
    req.flush([]);
  });
});