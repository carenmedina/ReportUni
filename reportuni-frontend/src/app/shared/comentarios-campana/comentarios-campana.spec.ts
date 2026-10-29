import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { ComentariosCampana } from './comentarios-campana';
import { AuthService } from '../../core/services/auth.service';

describe('ComentariosCampana', () => {
  let http: HttpTestingController;

  const nuevo = { id: 1, reporteId: 5, codigo: '0005', mensaje: 'Admin Uno comentó en tu reporte #0005: «Hola»', leida: false, fechaCreacion: '2026-10-22T08:00:00' };
  const viejo = { id: 2, reporteId: 6, codigo: '0006', mensaje: 'Admin Uno comentó en tu reporte #0006: «Gracias»', leida: true, fechaCreacion: '2026-10-21T08:00:00' };

  function crear(rol: 'ESTUDIANTE' | 'ADMINISTRADOR' = 'ESTUDIANTE') {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: { usuarioActual: () => ({ nombreCompleto: 'Persona', rol }) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(ComentariosCampana);
    fixture.detectChanges();
    http.expectOne((r) => r.url.endsWith('/notificaciones/comentarios/no-leidas')).flush({ total: 2 });
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => http.verify());

  it('muestra el contador de comentarios sin leer', () => {
    const fixture = crear();
    expect((fixture.nativeElement as HTMLElement).querySelector('.ru-campana__contador')?.textContent?.trim()).toBe('2');
    fixture.destroy();
  });

  it('al abrir carga los avisos de comentarios', () => {
    const fixture = crear();
    fixture.componentInstance.alternar();
    http.expectOne((r) => r.url.endsWith('/notificaciones/comentarios')).flush([nuevo, viejo]);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelectorAll('.ru-campana__item').length).toBe(2);
    expect(el.querySelectorAll('.ru-campana__item--nueva').length).toBe(1);
    fixture.destroy();
  });

  it('muestra un texto cuando no hay comentarios', () => {
    const fixture = crear();
    fixture.componentInstance.alternar();
    http.expectOne((r) => r.url.endsWith('/notificaciones/comentarios')).flush([]);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Aún no hay comentarios en tus reportes.');
    fixture.destroy();
  });

  it('muestra un mensaje si no se pueden cargar', () => {
    const fixture = crear();
    fixture.componentInstance.alternar();
    http.expectOne((r) => r.url.endsWith('/notificaciones/comentarios'))
      .flush({}, { status: 500, statusText: 'Error' });
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No pudimos cargar los comentarios.');
    fixture.destroy();
  });

  it('al abrir un comentario lo marca leido y lleva al reporte del estudiante', () => {
    const fixture = crear();
    const navegar = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture.componentInstance.alternar();
    http.expectOne((r) => r.url.endsWith('/notificaciones/comentarios')).flush([nuevo, viejo]);

    fixture.componentInstance.abrirComentario(nuevo);
    http.expectOne((r) => r.url.endsWith('/notificaciones/1/leida')).flush(null);

    expect(navegar).toHaveBeenCalledWith(['/mis-reportes', 5]);
    expect(fixture.componentInstance.noLeidos()).toBe(0);
    fixture.destroy();
  });

  it('el administrador va al detalle de gestion', () => {
    const fixture = crear('ADMINISTRADOR');
    const navegar = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

    fixture.componentInstance.abrirComentario(viejo); // ya leido: no pide nada

    expect(navegar).toHaveBeenCalledWith(['/admin/reportes', 6]);
    fixture.destroy();
  });

  it('marca todos como leidos', () => {
    const fixture = crear();
    fixture.componentInstance.alternar();
    http.expectOne((r) => r.url.endsWith('/notificaciones/comentarios')).flush([nuevo, viejo]);

    fixture.componentInstance.marcarTodos();
    const req = http.expectOne((r) => r.url.endsWith('/notificaciones/comentarios/leidas'));
    expect(req.request.method).toBe('PUT');
    req.flush(null);

    expect(fixture.componentInstance.noLeidos()).toBe(0);
    fixture.destroy();
  });
});