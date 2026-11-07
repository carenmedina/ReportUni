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
    http.match(() => true).forEach((r) => {
      if (r.request.url.endsWith('/perfil')) {
        r.flush({ nombreCompleto: 'Estudiante Uno', correoInstitucional: 'e1@fet.edu.co', rol: 'ESTUDIANTE', tieneFoto: false,
          resumen: { total: 0, pendientes: 0, enProceso: 0, resueltos: 0, gestionados: null } });
      } else {
        r.flush(r.request.url.endsWith('/no-leidas') ? { total: 0 } : { items: [] });
      }
    });
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

  it('el boton de usuario muestra las iniciales y el menu trae Mi perfil antes de Cerrar sesion', () => {
    const fixture = crear();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('.ru-avatar')?.textContent?.trim()).toBe('EU');

    (el.querySelector('.ru-header__user') as HTMLButtonElement).click();
    fixture.detectChanges();

    const enlace = el.querySelector('.ru-usermenu__link') as HTMLAnchorElement;
    expect(enlace.textContent).toContain('Mi perfil');
    expect(enlace.getAttribute('href')).toBe('/perfil');
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