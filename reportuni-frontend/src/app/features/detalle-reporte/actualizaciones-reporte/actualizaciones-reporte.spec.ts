import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActualizacionesReporte } from './actualizaciones-reporte';

describe('ActualizacionesReporte', () => {
  let http: HttpTestingController;

  const comentario = (id: number, texto: string, propio: boolean, fecha: string) => ({
    id, reporteId: 5, texto, autor: propio ? 'Estudiante Uno' : 'Ana Admin',
    rol: propio ? 'ESTUDIANTE' : 'ADMINISTRADOR', propio, fecha,
  });

  function crear(historial: unknown[], comentarios: unknown[] = [], conError = false) {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(ActualizacionesReporte);
    fixture.componentRef.setInput('reporteId', 5);
    fixture.componentRef.setInput('historial', historial);
    fixture.componentRef.setInput('conError', conError);
    fixture.detectChanges();
    http.expectOne((r) => r.url.endsWith('/reportes/5/comentarios') && r.method === 'GET').flush(comentarios);
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => http.verify());

  it('muestra el texto vacio cuando no hay nada', () => {
    const f = crear([]);
    expect((f.nativeElement as HTMLElement).textContent).toContain('Aún no hay comentarios ni actualizaciones del proceso.');
    f.destroy();
  });

  it('muestra el aviso de error si no se pudieron cargar las actualizaciones', () => {
    const f = crear([], [], true);
    expect((f.nativeElement as HTMLElement).textContent).toContain('No pudimos cargar las actualizaciones del proceso.');
    f.destroy();
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
    f.destroy();
  });

  it('mezcla comentarios y cambios en orden cronologico y marca los propios con (yo)', () => {
    const f = crear(
      [{ id: 1, tipo: 'ESTADO', valorAnterior: 'PENDIENTE', valorNuevo: 'EN_PROCESO', nota: 'Técnico asignado', realizadoPor: 'Ana Admin', fecha: '2026-10-21T10:00:00', fotos: [] }],
      [comentario(1, 'Gracias por avisar', true, '2026-10-21T11:00:00'), comentario(2, 'Primer mensaje', false, '2026-10-21T09:00:00')],
    );
    const textos = Array.from((f.nativeElement as HTMLElement).querySelectorAll('.ar__item')).map((li) => li.textContent!);

    expect(textos.length).toBe(3);
    expect(textos[0]).toContain('Primer mensaje');
    expect(textos[1]).toContain('Técnico asignado');
    expect(textos[2]).toContain('Gracias por avisar');
    expect(textos[2]).toContain('Estudiante Uno (yo)');
    expect(textos[0]).toContain('Administración');
    f.destroy();
  });

  it('no envia un comentario vacio', () => {
    const f = crear([]);
    f.componentInstance.cambiarTexto('   ');
    f.componentInstance.enviar();
    http.expectNone((r) => r.method === 'POST');
    expect(f.componentInstance.errorEnvio()).toBe('Escribe un comentario antes de enviarlo.');
    f.destroy();
  });

  it('no envia un comentario de mas de 1000 caracteres', () => {
    const f = crear([]);
    f.componentInstance.cambiarTexto('a'.repeat(1001));
    f.componentInstance.enviar();
    http.expectNone((r) => r.method === 'POST');
    expect(f.componentInstance.errorEnvio()).toContain('1000');
    f.destroy();
  });

  it('envia el comentario, lo agrega a la lista y limpia el campo', () => {
    const f = crear([]);
    f.componentInstance.cambiarTexto('  Hola, ¿hay novedades?  ');
    f.componentInstance.enviar();

    const req = http.expectOne((r) => r.url.endsWith('/reportes/5/comentarios') && r.method === 'POST');
    expect(req.request.body).toEqual({ texto: 'Hola, ¿hay novedades?' });
    expect(f.componentInstance.enviando()).toBeTrue();
    req.flush(comentario(7, 'Hola, ¿hay novedades?', true, '2026-10-21T12:00:00'));
    f.detectChanges();

    expect(f.componentInstance.enviando()).toBeFalse();
    expect(f.componentInstance.texto()).toBe('');
    expect((f.nativeElement as HTMLElement).textContent).toContain('Hola, ¿hay novedades?');
    f.destroy();
  });

  it('muestra el mensaje del servidor si falla y conserva lo escrito', () => {
    const f = crear([]);
    f.componentInstance.cambiarTexto('Hola');
    f.componentInstance.enviar();
    http.expectOne((r) => r.method === 'POST')
      .flush({ mensaje: 'No encontramos ese reporte.' }, { status: 404, statusText: 'Not Found' });

    expect(f.componentInstance.errorEnvio()).toBe('No encontramos ese reporte.');
    expect(f.componentInstance.texto()).toBe('Hola');
    expect(f.componentInstance.enviando()).toBeFalse();
    f.destroy();
  });

  it('avisa si no pudo cargar los comentarios y permite reintentar', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    const f = TestBed.createComponent(ActualizacionesReporte);
    f.componentRef.setInput('reporteId', 5);
    f.componentRef.setInput('historial', []);
    f.detectChanges();
    http.expectOne((r) => r.method === 'GET').flush({}, { status: 500, statusText: 'Error' });
    f.detectChanges();
    expect((f.nativeElement as HTMLElement).textContent).toContain('No pudimos cargar los comentarios.');

    f.componentInstance.reintentar();
    http.expectOne((r) => r.method === 'GET').flush([comentario(1, 'Hola', false, '2026-10-21T09:00:00')]);
    f.detectChanges();
    expect((f.nativeElement as HTMLElement).textContent).not.toContain('No pudimos cargar los comentarios.');
    expect((f.nativeElement as HTMLElement).textContent).toContain('Hola');
    f.destroy();
  });
});