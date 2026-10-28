import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Layout } from './layout';
import { AuthService } from '../../core/services/auth.service';

describe('Layout', () => {
  let http: HttpTestingController;
  const logout = jasmine.createSpy('logout');

  function crear() {
    logout.calls.reset();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: { usuarioActual: signal({ nombreCompleto: 'Estudiante Uno', rol: 'ESTUDIANTE' }), logout } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(Layout);
    fixture.detectChanges();
    http.match(() => true).forEach((r) => r.flush(r.request.url.endsWith('/no-leidas') ? { total: 0 } : { items: [] }));
    fixture.detectChanges();
    return fixture;
  }

  it('solo deja Reportes en la barra y muestra el logo de la FET', () => {
    const fixture = crear();
    const el = fixture.nativeElement as HTMLElement;
    const texto = el.querySelector('.ru-header')!.textContent!;

    ['Institucional', 'Académico', 'Resultados de evaluación', 'Estado de cuenta', 'Q10'].forEach((t) =>
      expect(texto).not.toContain(t),
    );
    expect(texto).toContain('Reportes');
    expect(el.querySelector('.ru-header__logo-img')?.getAttribute('src')).toBe('logo-fet.png');
    fixture.destroy();
  });

  it('el nombre abre un menu con Cerrar sesion y no cierra la sesion directamente', () => {
    const fixture = crear();
    const el = fixture.nativeElement as HTMLElement;

    (el.querySelector('.ru-header__user') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(logout).not.toHaveBeenCalled();
    const opcion = el.querySelector('.ru-usermenu__item') as HTMLButtonElement;
    expect(opcion.textContent).toContain('Cerrar sesión');

    opcion.click();
    expect(logout).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });

  it('el menu se cierra con Escape y al hacer clic fuera', () => {
    const fixture = crear();
    const el = fixture.nativeElement as HTMLElement;

    (el.querySelector('.ru-header__user') as HTMLButtonElement).click();
    fixture.detectChanges();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    expect(el.querySelector('.ru-usermenu__panel')).toBeNull();

    (el.querySelector('.ru-header__user') as HTMLButtonElement).click();
    fixture.detectChanges();
    document.body.click();
    fixture.detectChanges();
    expect(el.querySelector('.ru-usermenu__panel')).toBeNull();
    fixture.destroy();
  });
});