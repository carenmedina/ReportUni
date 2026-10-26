import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { DetalleReporte } from './detalle-reporte';

describe('DetalleReporte', () => {
  let http: HttpTestingController;

  const detalle = {
    id: 5, codigo: '0005', tipoDano: 'ASEO', bloque: 'BIBLIOTECA', espacioEspecifico: 'Sala 1',
    prioridadEstimada: 'MEDIA', estado: 'PENDIENTE', descripcion: 'Basura acumulada', latitud: null,
    longitud: null, notificarPorCorreo: true, reportadoPor: 'Estudiante Uno',
    fechaCreacion: '2026-10-20T09:30:00', fotos: [],
  };

  let historialInicial: unknown[] = [];

  beforeEach(() => (historialInicial = []));

  function crear(data: Record<string, unknown>) {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '5' }), data } } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(DetalleReporte);
    http.expectOne((r) => r.url.endsWith('/reportes/5')).flush(detalle);
    http.expectOne((r) => r.url.endsWith('/reportes/5/historial')).flush(historialInicial);
    fixture.detectChanges();
    return fixture;
  }

  function selector(valor: string): HTMLSelectElement {
    const el = document.createElement('select');
    ['PENDIENTE', 'EN_PROCESO', 'RESUELTO', 'ALTA', 'MEDIA', 'BAJA'].forEach((v) => {
      const op = document.createElement('option');
      op.value = v;
      el.appendChild(op);
    });
    el.value = valor;
    return el;
  }

  afterEach(() => http.verify());

  it('el estudiante no ve el panel de gestion', () => {
    const fixture = crear({});
    const el = fixture.nativeElement as HTMLElement;

    expect(fixture.componentInstance.gestionar).toBeFalse();
    expect(el.textContent).not.toContain('Gestionar reporte');
    expect(el.textContent).toContain('Basura acumulada');
  });

  it('el administrador ve el panel y puede cambiar el estado', () => {
    const fixture = crear({ gestionar: true, volverA: '/admin/reportes', volverTexto: 'Reportes (gestión)' });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Gestionar reporte');
    expect(el.textContent).toContain('Reportes (gestión)');

    fixture.componentInstance.cambiarEstado(selector('EN_PROCESO'));
    const req = http.expectOne((r) => r.url.endsWith('/admin/reportes/5/estado'));
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ estado: 'EN_PROCESO' });
    req.flush({ ...detalle, estado: 'EN_PROCESO', reportadoPor: 'Estudiante Uno', reportadoPorCorreo: 'e1@fet.edu.co' });
    // Semana 8: despues de guardar se vuelve a pedir el historial
    http.expectOne((r) => r.url.endsWith('/reportes/5/historial')).flush([]);

    expect(fixture.componentInstance.reporte()?.estado).toBe('EN_PROCESO');
    expect(fixture.componentInstance.aviso()?.tipo).toBe('ok');
  });

  it('muestra el error si el servidor rechaza el cambio de prioridad', () => {
    const fixture = crear({ gestionar: true });
    const sel = selector('ALTA');

    fixture.componentInstance.cambiarPrioridad(sel);
    http.expectOne((r) => r.url.endsWith('/admin/reportes/5/prioridad'))
      .flush({ mensaje: 'El reporte ya tiene prioridad Alta.' }, { status: 400, statusText: 'Bad Request' });

    expect(sel.value).toBe('MEDIA');
    expect(fixture.componentInstance.aviso()).toEqual({ tipo: 'error', texto: 'El reporte ya tiene prioridad Alta.' });
    expect(fixture.componentInstance.reporte()?.prioridadEstimada).toBe('MEDIA');
  });

  it('muestra el historial de cambios del reporte', () => {
    historialInicial = [
      { id: 1, tipo: 'ESTADO', valorAnterior: 'PENDIENTE', valorNuevo: 'EN_PROCESO', realizadoPor: 'Administrador', fecha: '2026-10-21T10:00:00' },
    ];
    const fixture = crear({});
    const el = fixture.nativeElement as HTMLElement;

    expect(el.textContent).toContain('Historial de cambios');
    expect(el.textContent).toContain('Estado: Pendiente → En proceso');
    expect(el.textContent).toContain('Administrador');
  });

  it('indica cuando aun no hay cambios', () => {
    const fixture = crear({});
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Aún no hay cambios registrados.');
  });
});