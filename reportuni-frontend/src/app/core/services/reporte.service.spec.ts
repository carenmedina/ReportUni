import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReporteService } from './reporte.service';
import { NuevoReporteDatos } from '../models/nuevo-reporte.models';

describe('ReporteService', () => {
  let servicio: ReporteService;
  let http: HttpTestingController;

  const datos: NuevoReporteDatos = {
    tipoDano: 'FUGA_AGUA',
    prioridadEstimada: 'MEDIA',
    bloque: 'BLOQUE_A',
    espacioEspecifico: 'Salón 204',
    descripcion: 'Fuga de agua en el techo del salón',
    latitud: 10.5,
    longitud: -73.2,
    notificarPorCorreo: true,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(ReporteService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envía multipart con los campos y las fotos', () => {
    const foto = new File(['x'], 'a.png', { type: 'image/png' });
    servicio.crear(datos, [foto]).subscribe();

    const req = http.expectOne((r) => r.url.endsWith('/reportes'));
    expect(req.request.method).toBe('POST');
    const fd = req.request.body as FormData;
    expect(fd.get('tipoDano')).toBe('FUGA_AGUA');
    expect(fd.get('latitud')).toBe('10.5');
    expect(fd.getAll('fotos').length).toBe(1);
    req.flush({ id: 1, codigo: '0001', estado: 'PENDIENTE', fechaCreacion: '', totalFotos: 1 });
  });

  it('omite latitud y longitud si no hay GPS', () => {
    servicio.crear({ ...datos, latitud: null, longitud: null }, []).subscribe();
    const req = http.expectOne((r) => r.url.endsWith('/reportes'));
    const fd = req.request.body as FormData;
    expect(fd.has('latitud')).toBeFalse();
    req.flush({});
  });

  it('interpreta errores 400, 403 y sin conexión', () => {
    const mk = (status: number, error: unknown) => ({ status, error }) as never;
    expect(servicio.interpretarError(mk(400, { mensaje: 'Malo', errores: { bloque: 'x' } })).errores['bloque']).toBe('x');
    expect(servicio.interpretarError(mk(403, null)).mensaje).toContain('permiso');
    expect(servicio.interpretarError(mk(0, null)).mensaje).toContain('conectar');
  });
});