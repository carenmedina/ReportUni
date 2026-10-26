import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { NotificacionesCampana } from './notificaciones-campana';
import { AuthService } from '../../core/services/auth.service';

describe('NotificacionesCampana', () => {
  let http: HttpTestingController;

  const nueva = { id: 1, reporteId: 5, codigo: '0005', mensaje: 'Tu reporte #0005 cambió a estado «Resuelto».', leida: false, fechaCreacion: '2026-10-22T08:00:00' };
  const vieja = { id: 2, reporteId: 6, codigo: '0006', mensaje: 'Otra', leida: true, fechaCreacion: '2026-10-21T08:00:00' };

  function crear() {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: { usuarioActual: () => ({ nombreCompleto: 'Estudiante Uno', rol: 'ESTUDIANTE' }) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(NotificacionesCampana);
    fixture.detectChanges();
    http.expectOne((r) => r.url.endsWith('/notificaciones/no-leidas')).flush({ total: 1 });
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => http.verify());

  it('muestra el contador de no leidas', () => {
    const fixture = crear();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.ru-campana__contador')?.textContent?.trim()).toBe('1');
    fixture.destroy();
  });

  it('al abrir carga la lista de notificaciones', () => {
    const fixture = crear();
    fixture.componentInstance.alternar();
    http.expectOne((r) => r.url.endsWith('/notificaciones')).flush([nueva, vieja]);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelectorAll('.ru-campana__item').length).toBe(2);
    expect(el.querySelectorAll('.ru-campana__item--nueva').length).toBe(1);
    fixture.destroy();
  });

  it('al abrir una notificacion la marca leida y navega al reporte', () => {
    const fixture = crear();
    const router = TestBed.inject(Router);
    const navegar = spyOn(router, 'navigate').and.resolveTo(true);

    fixture.componentInstance.alternar();
    http.expectOne((r) => r.url.endsWith('/notificaciones')).flush([nueva, vieja]);

    fixture.componentInstance.abrirNotificacion(nueva);
    http.expectOne((r) => r.url.endsWith('/notificaciones/1/leida')).flush(null);

    expect(navegar).toHaveBeenCalledWith(['/mis-reportes', 5]);
    expect(fixture.componentInstance.noLeidas()).toBe(0);
    expect(fixture.componentInstance.lista()[0].leida).toBeTrue();
    fixture.destroy();
  });

  it('marca todas como leidas', () => {
    const fixture = crear();
    fixture.componentInstance.alternar();
    http.expectOne((r) => r.url.endsWith('/notificaciones')).flush([nueva, vieja]);

    fixture.componentInstance.marcarTodas();
    http.expectOne((r) => r.url.endsWith('/notificaciones/leidas')).flush(null);

    expect(fixture.componentInstance.noLeidas()).toBe(0);
    fixture.destroy();
  });
});