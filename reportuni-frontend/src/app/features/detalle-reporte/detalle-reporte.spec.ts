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
    longitud: null, asignadoA: null, ultimaActualizacion: '2026-10-20T09:30:00', reportadoPor: 'Estudiante Uno',
    fechaCreacion: '2026-10-20T09:30:00', fotos: [],
  };

  let historialInicial: unknown[] = [];
  let comentariosIniciales: unknown[] = [];

  beforeEach(() => {
    historialInicial = [];
    comentariosIniciales = [];
  });

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
    // Fase 3: la tarjeta de comentarios pide los comentarios del reporte
    http.expectOne((r) => r.url.endsWith('/reportes/5/comentarios')).flush(comentariosIniciales);
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => http.verify());

  it('el estudiante no ve el panel de gestion', () => {
    const fixture = crear({});
    const el = fixture.nativeElement as HTMLElement;

    expect(fixture.componentInstance.gestionar).toBeFalse();
    expect(el.textContent).not.toContain('Gestionar reporte');
    expect(el.textContent).toContain('Basura acumulada');
  });

  it('el administrador ve el panel de gestion', () => {
    const fixture = crear({ gestionar: true, volverA: '/admin/reportes', volverTexto: 'Reportes (gestión)' });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Gestionar reporte');
    expect(el.textContent).toContain('Reportes (gestión)');
    expect(el.textContent).toContain('Publicar actualización');
  });

  it('refrescar vuelve a pedir el reporte y su historial', () => {
    const fixture = crear({ gestionar: true });
    fixture.componentInstance.refrescar();
    http.expectOne((r) => r.url.endsWith('/reportes/5')).flush({ ...detalle, estado: 'EN_PROCESO', asignadoA: 'SOPORTE_TECNICO' });
    http.expectOne((r) => r.url.endsWith('/reportes/5/historial')).flush([]);
    expect(fixture.componentInstance.reporte()?.estado).toBe('EN_PROCESO');
  });

  it('muestra las actualizaciones, la linea de tiempo y el campo Asignado a', () => {
    historialInicial = [
      { id: 2, tipo: 'ESTADO', valorAnterior: 'PENDIENTE', valorNuevo: 'EN_PROCESO', nota: 'Técnico en camino', realizadoPor: 'Administrador', fecha: '2026-10-21T10:00:00', fotos: [] },
      { id: 1, tipo: 'ASIGNACION', valorAnterior: '', valorNuevo: 'SOPORTE_TECNICO', nota: null, realizadoPor: 'Administrador', fecha: '2026-10-20T10:00:00', fotos: [] },
    ];
    const fixture = crear({});
    const el = fixture.nativeElement as HTMLElement;

    expect(el.textContent).toContain('Estado: Pendiente → En proceso');
    expect(el.textContent).toContain('Técnico en camino');
    expect(el.textContent).toContain('Asignando a Soporte Técnico');
    expect(el.textContent).toContain('Asignado a');
    expect(el.textContent).toContain('Última actualización');
  });

  it('indica cuando aun no hay comentarios ni actualizaciones', () => {
    const fixture = crear({});
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Aún no hay comentarios ni actualizaciones del proceso.');
  });

  it('muestra los comentarios del reporte junto a las actualizaciones', () => {
    comentariosIniciales = [
      { id: 1, reporteId: 5, texto: 'Ya enviamos al técnico', autor: 'Ana Admin', rol: 'ADMINISTRADOR', propio: false, fecha: '2026-10-21T11:00:00' },
      { id: 2, reporteId: 5, texto: 'Muchas gracias', autor: 'Estudiante Uno', rol: 'ESTUDIANTE', propio: true, fecha: '2026-10-21T12:00:00' },
    ];
    const fixture = crear({});
    const texto = (fixture.nativeElement as HTMLElement).textContent!;

    expect(texto).toContain('Comentarios y actualizaciones');
    expect(texto).toContain('Ya enviamos al técnico');
    expect(texto).toContain('Estudiante Uno (yo)');
  });
});