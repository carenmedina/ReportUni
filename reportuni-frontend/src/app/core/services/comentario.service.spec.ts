import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComentarioService } from './comentario.service';

describe('ComentarioService', () => {
  let servicio: ComentarioService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(ComentarioService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pide los comentarios de un reporte', () => {
    servicio.listar(5).subscribe((l) => expect(l.length).toBe(1));
    const req = http.expectOne((r) => r.url.endsWith('/reportes/5/comentarios'));
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, reporteId: 5, texto: 'Hola', autor: 'Admin', rol: 'ADMINISTRADOR', propio: false, fecha: '2026-10-21T10:00:00' }]);
  });

  it('envia un comentario con POST', () => {
    servicio.comentar(5, 'Gracias').subscribe();
    const req = http.expectOne((r) => r.url.endsWith('/reportes/5/comentarios'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ texto: 'Gracias' });
    req.flush({});
  });

  it('pide los avisos, el total y marca todos como leidos', () => {
    servicio.avisos().subscribe();
    http.expectOne((r) => r.url.endsWith('/notificaciones/comentarios')).flush([]);

    servicio.totalNoLeidos().subscribe((t) => expect(t.total).toBe(3));
    http.expectOne((r) => r.url.endsWith('/notificaciones/comentarios/no-leidas')).flush({ total: 3 });

    servicio.marcarTodosLeidos().subscribe();
    const req = http.expectOne((r) => r.url.endsWith('/notificaciones/comentarios/leidas'));
    expect(req.request.method).toBe('PUT');
    req.flush(null);
  });

  it('usa el mensaje del servidor en un 400 y textos claros en los demas errores', () => {
    expect(servicio.interpretarError({ status: 400, error: { mensaje: 'Escribe un comentario antes de enviarlo.' } } as any))
      .toBe('Escribe un comentario antes de enviarlo.');
    expect(servicio.interpretarError({ status: 404, error: null } as any)).toBe('No encontramos ese reporte.');
    expect(servicio.interpretarError({ status: 0, error: null } as any)).toContain('No se pudo conectar');
    expect(servicio.interpretarError({ status: 500, error: null } as any)).toContain('No pudimos enviar');
  });
});