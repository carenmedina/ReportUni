import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AdminReportes } from './admin-reportes';

describe('AdminReportes', () => {
  let http: HttpTestingController;

  const datos = [
    { id: 2, codigo: '0002', tipoDano: 'AUDIOVISUAL', bloque: 'BLOQUE_C', espacioEspecifico: 'Auditorio',
      prioridadEstimada: 'MEDIA', estado: 'EN_PROCESO', reportadoPor: 'Estudiante Dos',
      reportadoPorCorreo: 'estudiante2@fet.edu.co', fechaCreacion: '2026-10-05T09:30:00' },
    { id: 1, codigo: '0001', tipoDano: 'FUGA_AGUA', bloque: 'BLOQUE_A', espacioEspecifico: 'Salón 204',
      prioridadEstimada: 'ALTA', estado: 'PENDIENTE', reportadoPor: 'Estudiante Uno',
      reportadoPorCorreo: 'estudiante1@fet.edu.co', fechaCreacion: '2026-10-04T08:00:00' },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function crear() {
    const fixture = TestBed.createComponent(AdminReportes);
    http.expectOne((r) => r.url.endsWith('/admin/reportes')).flush(datos);
    fixture.detectChanges();
    return fixture;
  }

  it('muestra todos los reportes con quien los reporto', () => {
    const fixture = crear();
    const el = fixture.nativeElement as HTMLElement;

    expect(fixture.componentInstance.cargando()).toBeFalse();
    expect(el.querySelectorAll('tr').length).toBe(3); // encabezado + 2 reportes
    expect(el.textContent).toContain('Estudiante Uno');
    expect(el.textContent).toContain('estudiante2@fet.edu.co');
  });

  it('al elegir un filtro vuelve a pedir al servidor con ese filtro', () => {
    const fixture = crear();

    fixture.componentInstance.cambiarFiltro('estado', 'PENDIENTE');
    const req = http.expectOne((r) => r.url.endsWith('/admin/reportes'));
    expect(req.request.params.get('estado')).toBe('PENDIENTE');
    req.flush([datos[1]]);
    fixture.detectChanges();

    expect(fixture.componentInstance.reportes().length).toBe(1);
    expect(fixture.componentInstance.hayFiltros()).toBeTrue();
  });

  it('limpiar filtros restablece y recarga todo', () => {
    const fixture = crear();
    fixture.componentInstance.cambiarFiltro('prioridad', 'ALTA');
    http.expectOne((r) => r.url.endsWith('/admin/reportes')).flush([datos[1]]);

    fixture.componentInstance.limpiarFiltros();
    const req = http.expectOne((r) => r.url.endsWith('/admin/reportes'));
    expect(req.request.params.keys().length).toBe(0);
    req.flush(datos);

    expect(fixture.componentInstance.hayFiltros()).toBeFalse();
  });

  it('muestra un mensaje si el servidor no responde', () => {
    const fixture = TestBed.createComponent(AdminReportes);
    http.expectOne((r) => r.url.endsWith('/admin/reportes')).error(new ProgressEvent('error'), { status: 0 });
    fixture.detectChanges();

    expect(fixture.componentInstance.error()).toContain('No se pudo conectar');
  });
});