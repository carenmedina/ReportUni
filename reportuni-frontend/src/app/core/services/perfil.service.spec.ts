import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PerfilService } from './perfil.service';
import { Perfil } from '../models/perfil.model';

describe('PerfilService', () => {
  let servicio: PerfilService;
  let http: HttpTestingController;

  const perfil = (tieneFoto: boolean): Perfil => ({
    nombreCompleto: 'Estudiante Uno',
    correoInstitucional: 'estudiante1@fet.edu.co',
    rol: 'ESTUDIANTE',
    tieneFoto,
    resumen: { total: 3, pendientes: 1, enProceso: 1, resueltos: 1, gestionados: null },
  });

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(PerfilService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('carga el perfil y no pide la foto si no tiene', () => {
    servicio.cargar();
    http.expectOne((r) => r.url.endsWith('/perfil')).flush(perfil(false));

    expect(servicio.perfil()?.nombreCompleto).toBe('Estudiante Uno');
    expect(servicio.fotoUrl()).toBeNull();
  });

  it('si tiene foto la descarga como blob y la deja lista para mostrar', () => {
    servicio.cargar();
    http.expectOne((r) => r.url.endsWith('/perfil')).flush(perfil(true));

    const req = http.expectOne((r) => r.url.endsWith('/perfil/foto'));
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['x'], { type: 'image/png' }));

    expect(servicio.fotoUrl()).toContain('blob:');
  });

  it('sube la foto con POST multipart y vuelve a descargarla', () => {
    servicio.subirFoto(new File(['x'], 'yo.png', { type: 'image/png' })).subscribe();

    const req = http.expectOne((r) => r.url.endsWith('/perfil/foto') && r.method === 'POST');
    expect((req.request.body as FormData).get('foto')).toBeTruthy();
    req.flush(perfil(true));

    http.expectOne((r) => r.url.endsWith('/perfil/foto') && r.method === 'GET').flush(new Blob(['x']));
    expect(servicio.perfil()?.tieneFoto).toBeTrue();
    expect(servicio.fotoUrl()).toContain('blob:');
  });

  it('quita la foto con DELETE y limpia la vista', () => {
    servicio.fotoUrl.set('blob:algo');
    servicio.quitarFoto().subscribe();

    const req = http.expectOne((r) => r.url.endsWith('/perfil/foto'));
    expect(req.request.method).toBe('DELETE');
    req.flush(perfil(false));

    expect(servicio.fotoUrl()).toBeNull();
    expect(servicio.perfil()?.tieneFoto).toBeFalse();
  });

  it('cambia la contrasena con PUT', () => {
    servicio.cambiarPassword({ actual: 'a', nueva: 'NuevaClave2026', confirmacion: 'NuevaClave2026' }).subscribe();

    const req = http.expectOne((r) => r.url.endsWith('/perfil/password'));
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.nueva).toBe('NuevaClave2026');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('limpiar borra el perfil y la foto', () => {
    servicio.perfil.set(perfil(true));
    servicio.fotoUrl.set('blob:algo');
    servicio.limpiar();

    expect(servicio.perfil()).toBeNull();
    expect(servicio.fotoUrl()).toBeNull();
  });

  it('usa el mensaje del servidor en un 400 y textos claros en los demas errores', () => {
    expect(servicio.interpretarError({ status: 400, error: { mensaje: 'La contraseña actual no es correcta.' } } as any, 'password'))
      .toBe('La contraseña actual no es correcta.');
    expect(servicio.interpretarError({ status: 413, error: null } as any, 'foto')).toContain('5 MB');
    expect(servicio.interpretarError({ status: 0, error: null } as any, 'foto')).toContain('No se pudo conectar');
    expect(servicio.interpretarError({ status: 500, error: null } as any, 'foto')).toContain('foto');
    expect(servicio.interpretarError({ status: 500, error: null } as any, 'password')).toContain('contraseña');
  });
});