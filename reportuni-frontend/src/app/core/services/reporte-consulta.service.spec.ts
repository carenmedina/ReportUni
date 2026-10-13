import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReporteConsultaService } from './reporte-consulta.service';

describe('ReporteConsultaService', () => {
  let servicio: ReporteConsultaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(ReporteConsultaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pide el listado de mis reportes', () => {
    servicio.listarMios().subscribe((lista) => expect(lista.length).toBe(1));

    const req = http.expectOne((r) => r.url.endsWith('/reportes/mis'));
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, codigo: '0001' }]);
  });

  it('pide el detalle de un reporte por id', () => {
    servicio.obtenerDetalle(12).subscribe();

    const req = http.expectOne((r) => r.url.endsWith('/reportes/12'));
    expect(req.request.method).toBe('GET');
    req.flush({ id: 12, codigo: '0012', fotos: [] });
  });

  it('pide la foto como blob', () => {
    servicio.obtenerFoto(12, 70).subscribe();

    const req = http.expectOne((r) => r.url.endsWith('/reportes/12/fotos/70'));
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['x'], { type: 'image/jpeg' }));
  });

  it('traduce los errores HTTP a mensajes para el usuario', () => {
    expect(servicio.interpretarError(new HttpErrorResponse({ status: 404 }))).toContain('No encontramos');
    expect(servicio.interpretarError(new HttpErrorResponse({ status: 0 }))).toContain('servidor');
    expect(servicio.interpretarError(new HttpErrorResponse({ status: 500 }))).toContain('inesperado');
  });
});