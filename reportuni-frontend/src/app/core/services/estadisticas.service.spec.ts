import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { EstadisticasService } from './estadisticas.service';

describe('EstadisticasService', () => {
  let servicio: EstadisticasService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(EstadisticasService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sin rango no envia parametros', () => {
    servicio.obtener({ desde: '', hasta: '' }).subscribe();
    const req = http.expectOne((r) => r.url.endsWith('/admin/estadisticas'));
    expect(req.request.method).toBe('GET');
    expect(req.request.params.keys().length).toBe(0);
    req.flush({});
  });

  it('con rango envia desde y hasta', () => {
    servicio.obtener({ desde: '2026-10-01', hasta: '2026-10-31' }).subscribe();
    const req = http.expectOne((r) => r.url.endsWith('/admin/estadisticas'));
    expect(req.request.params.get('desde')).toBe('2026-10-01');
    expect(req.request.params.get('hasta')).toBe('2026-10-31');
    req.flush({});
  });

  it('usa el mensaje del servidor en un 400 y textos claros en los demas errores', () => {
    expect(servicio.interpretarError({ status: 400, error: { mensaje: 'La fecha inicial no puede ser posterior a la fecha final.' } } as any))
      .toBe('La fecha inicial no puede ser posterior a la fecha final.');
    expect(servicio.interpretarError({ status: 0, error: null } as any)).toContain('No se pudo conectar');
    expect(servicio.interpretarError({ status: 403, error: null } as any)).toContain('administrador');
    expect(servicio.interpretarError({ status: 500, error: null } as any)).toContain('No pudimos cargar');
  });
});