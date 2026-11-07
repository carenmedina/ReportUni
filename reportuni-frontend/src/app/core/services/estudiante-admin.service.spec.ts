import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { EstudianteAdminService } from './estudiante-admin.service';

describe('EstudianteAdminService', () => {
  let servicio: EstudianteAdminService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(EstudianteAdminService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envia el archivo como formulario al endpoint de carga', () => {
    const archivo = new File(['documento;nombre;correo'], 'estudiantes.csv', { type: 'text/csv' });
    servicio.cargar(archivo).subscribe((r) => expect(r.creados).toBe(2));

    const req = http.expectOne((r) => r.url.endsWith('/admin/estudiantes/carga'));
    expect(req.request.method).toBe('POST');
    expect((req.request.body as FormData).get('archivo')).toBeTruthy();
    req.flush({ filasLeidas: 2, creados: 2, actualizados: 0, rechazados: 0, errores: [] });
  });

  it('usa el mensaje del servidor en un 400 y textos claros en los demas errores', () => {
    expect(servicio.interpretarError({ status: 400, error: { mensaje: 'El archivo está vacío.' } } as any))
      .toBe('El archivo está vacío.');
    expect(servicio.interpretarError({ status: 413, error: null } as any)).toContain('2 MB');
    expect(servicio.interpretarError({ status: 0, error: null } as any)).toContain('No se pudo conectar');
    expect(servicio.interpretarError({ status: 403, error: null } as any)).toContain('administrador');
    expect(servicio.interpretarError({ status: 500, error: null } as any)).toContain('inesperado');
  });
});