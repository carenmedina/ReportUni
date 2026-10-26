import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SeguimientoService } from './seguimiento.service';
import { descripcionCambio, etiquetaValor } from '../models/seguimiento.model';

describe('SeguimientoService', () => {
  let servicio: SeguimientoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(SeguimientoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pide el historial de un reporte', () => {
    servicio.historial(5).subscribe((lista) => expect(lista.length).toBe(1));
    const req = http.expectOne((r) => r.url.endsWith('/reportes/5/historial'));
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, tipo: 'ESTADO', valorAnterior: 'PENDIENTE', valorNuevo: 'EN_PROCESO', realizadoPor: 'Admin', fecha: '2026-10-21T10:00:00' }]);
  });

  it('pide las notificaciones y el total sin leer', () => {
    servicio.notificaciones().subscribe();
    http.expectOne((r) => r.url.endsWith('/notificaciones')).flush([]);

    servicio.totalNoLeidas().subscribe((t) => expect(t.total).toBe(3));
    http.expectOne((r) => r.url.endsWith('/notificaciones/no-leidas')).flush({ total: 3 });
  });

  it('marca una y todas como leidas con PUT', () => {
    servicio.marcarLeida(4).subscribe();
    const uno = http.expectOne((r) => r.url.endsWith('/notificaciones/4/leida'));
    expect(uno.request.method).toBe('PUT');
    uno.flush(null);

    servicio.marcarTodasLeidas().subscribe();
    const todas = http.expectOne((r) => r.url.endsWith('/notificaciones/leidas'));
    expect(todas.request.method).toBe('PUT');
    todas.flush(null);
  });

  it('convierte los valores en texto legible', () => {
    expect(etiquetaValor('EN_PROCESO')).toBe('En proceso');
    expect(
      descripcionCambio({ id: 1, tipo: 'PRIORIDAD', valorAnterior: 'MEDIA', valorNuevo: 'ALTA', realizadoPor: 'A', fecha: '' }),
    ).toBe('Prioridad: Media → Alta');
  });
});