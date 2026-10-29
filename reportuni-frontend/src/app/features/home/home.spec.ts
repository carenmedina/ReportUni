import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Home } from './home';

describe('Home (administrador)', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('muestra los totales y los reportes recientes reales', () => {
    const fixture = TestBed.createComponent(Home);
    http.expectOne((r) => r.url.endsWith('/admin/reportes/resumen')).flush({
      total: 6, pendientes: 3, enProceso: 2, resueltos: 1, altaPrioridadPendientes: 2,
      recientes: [
        { id: 6, codigo: '0006', tipoDano: 'FUGA_AGUA', bloque: 'BLOQUE_1', espacioEspecifico: 'Salón 204',
          prioridadEstimada: 'ALTA', estado: 'PENDIENTE', reportadoPor: 'Estudiante Uno',
          reportadoPorCorreo: 'estudiante1@fet.edu.co', fechaCreacion: '2026-10-05T09:30:00' },
      ],
    });
    fixture.detectChanges();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Estudiante Uno');
    expect(texto).toContain('0006');
    expect(fixture.componentInstance.resumen()?.total).toBe(6);
    expect(fixture.componentInstance.cargando()).toBeFalse();
  });

  it('informa el error si falla la carga', () => {
    const fixture = TestBed.createComponent(Home);
    http.expectOne((r) => r.url.endsWith('/admin/reportes/resumen'))
      .flush({}, { status: 403, statusText: 'Forbidden' });
    fixture.detectChanges();

    expect(fixture.componentInstance.error()).toContain('administrador');
  });
});