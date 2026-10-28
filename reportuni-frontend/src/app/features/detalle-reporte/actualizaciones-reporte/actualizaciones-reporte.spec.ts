import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActualizacionesReporte } from './actualizaciones-reporte';

describe('ActualizacionesReporte', () => {
  let http: HttpTestingController;

  function crear(historial: unknown[], conError = false) {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(ActualizacionesReporte);
    fixture.componentRef.setInput('reporteId', 5);
    fixture.componentRef.setInput('historial', historial);
    fixture.componentRef.setInput('conError', conError);
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => http.verify());

  it('muestra el texto vacio cuando no hay actualizaciones', () => {
    const f = crear([]);
    expect((f.nativeElement as HTMLElement).textContent).toContain('Aún no hay actualizaciones del proceso.');
  });

  it('muestra el aviso de error si no se pudo cargar', () => {
    const f = crear([], true);
    expect((f.nativeElement as HTMLElement).textContent).toContain('No pudimos cargar las actualizaciones.');
  });

  it('muestra nota, autor y pide las fotos de la actualizacion', () => {
    const f = crear([
      { id: 3, tipo: 'ACTUALIZACION', valorAnterior: '', valorNuevo: '', nota: 'Llegó el repuesto', realizadoPor: 'Ana Admin', fecha: '2026-10-21T10:00:00',
        fotos: [{ id: 9, nombreOriginal: 'r.jpg', tipoContenido: 'image/jpeg', tamanoBytes: 10 }] },
    ]);
    http.expectOne((r) => r.url.endsWith('/reportes/5/historial/3/fotos/9')).flush(new Blob(['x']));
    f.detectChanges();
    const texto = (f.nativeElement as HTMLElement).textContent!;
    expect(texto).toContain('Llegó el repuesto');
    expect(texto).toContain('Ana Admin');
    expect(texto).toContain('Actualización del proceso');
  });
});