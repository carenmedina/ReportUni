import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { MisReportes } from './mis-reportes';

describe('MisReportes', () => {
  let http: HttpTestingController;

  const datos = [
    { id: 2, codigo: '0002', tipoDano: 'AUDIOVISUAL', bloque: 'BLOQUE_C', espacioEspecifico: 'Auditorio',
      prioridadEstimada: 'MEDIA', estado: 'EN_PROCESO', fechaCreacion: '2026-10-05T09:30:00', totalFotos: 1 },
    { id: 1, codigo: '0001', tipoDano: 'FUGA_AGUA', bloque: 'BLOQUE_A', espacioEspecifico: 'Salón 204',
      prioridadEstimada: 'ALTA', estado: 'PENDIENTE', fechaCreacion: '2026-10-04T08:00:00', totalFotos: 2 },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function crear() {
    const fixture = TestBed.createComponent(MisReportes);
    http.expectOne((r) => r.url.endsWith('/reportes/mis')).flush(datos);
    fixture.detectChanges();
    return fixture;
  }

  it('carga y muestra los reportes reales del servidor', () => {
    const fixture = crear();
    const filas = (fixture.nativeElement as HTMLElement).querySelectorAll('tr');

    expect(fixture.componentInstance.cargando()).toBeFalse();
    expect(filas.length).toBe(3); // encabezado + 2 reportes
  });

  it('filtra por estado', () => {
    const fixture = crear();
    fixture.componentInstance.actualizarFiltroEstado('PENDIENTE');

    expect(fixture.componentInstance.reportesFiltrados().length).toBe(1);
    expect(fixture.componentInstance.reportesFiltrados()[0].codigo).toBe('0001');
  });

  it('busca por tipo de daño', () => {
    const fixture = crear();
    fixture.componentInstance.actualizarBusqueda('audiovisual');

    expect(fixture.componentInstance.reportesFiltrados().length).toBe(1);
  });

  it('muestra un mensaje cuando el servidor falla', () => {
    const fixture = TestBed.createComponent(MisReportes);
    http.expectOne((r) => r.url.endsWith('/reportes/mis')).flush('x', { status: 500, statusText: 'Error' });

    expect(fixture.componentInstance.error()).toContain('inesperado');
  });
});