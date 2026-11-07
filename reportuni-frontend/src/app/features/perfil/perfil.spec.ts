import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PerfilPagina } from './perfil';
import { PerfilService } from '../../core/services/perfil.service';
import { Perfil } from '../../core/models/perfil.model';

describe('PerfilPagina', () => {
  let http: HttpTestingController;

  const estudiante: Perfil = {
    nombreCompleto: 'Caren Vanesa Medina',
    correoInstitucional: 'caren_medinana@fet.edu.co',
    rol: 'ESTUDIANTE',
    tieneFoto: false,
    resumen: { total: 6, pendientes: 3, enProceso: 2, resueltos: 1, gestionados: null },
  };

  const admin: Perfil = {
    nombreCompleto: 'Administrador ReportUni',
    correoInstitucional: 'admin@fet.edu.co',
    rol: 'ADMINISTRADOR',
    tieneFoto: false,
    resumen: { total: null, pendientes: null, enProceso: null, resueltos: null, gestionados: 14 },
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    TestBed.inject(PerfilService).limpiar();
    http.verify();
  });

  function crear(perfil: Perfil) {
    const fixture = TestBed.createComponent(PerfilPagina);
    fixture.detectChanges();
    http.expectOne((r) => r.url.endsWith('/perfil')).flush(perfil);
    fixture.detectChanges();
    return fixture;
  }

  function evento(archivo: File): Event {
    return { target: { files: [archivo], value: 'x' } } as unknown as Event;
  }

  it('muestra nombre, correo, rol e iniciales cuando no hay foto', () => {
    const fixture = crear(estudiante);
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(texto).toContain('Caren Vanesa Medina');
    expect(texto).toContain('caren_medinana@fet.edu.co');
    expect(texto).toContain('Estudiante');
    expect(fixture.componentInstance.iniciales()).toBe('CV');
  });

  it('el estudiante ve cuantos reportes tiene y en que estado', () => {
    const fixture = crear(estudiante);
    const cifras = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('.ru-perfil__cifra')).map((c) => ({
      numero: c.querySelector('strong')!.textContent!.trim(),
      etiqueta: c.querySelector('span')!.textContent!.trim(),
    }));

    expect(cifras).toEqual([
      { numero: '6', etiqueta: 'Total' },
      { numero: '3', etiqueta: 'Pendientes' },
      { numero: '2', etiqueta: 'En proceso' },
      { numero: '1', etiqueta: 'Resueltos' },
    ]);
  });

  it('el administrador ve cuantos reportes ha gestionado', () => {
    const fixture = crear(admin);
    const el = fixture.nativeElement as HTMLElement;

    expect(el.textContent).toContain('Administrador');
    expect(el.querySelectorAll('.ru-perfil__cifra').length).toBe(1);
    expect(el.querySelector('.ru-perfil__cifra')!.textContent).toContain('14');
    expect(el.textContent).toContain('Reportes gestionados');
  });

  it('rechaza fotos que no son JPG o PNG y las que superan 5 MB sin llamar al servidor', () => {
    const fixture = crear(estudiante);

    fixture.componentInstance.elegirFoto(evento(new File(['x'], 'doc.pdf', { type: 'application/pdf' })));
    expect(fixture.componentInstance.avisoFoto()?.texto).toContain('JPG o PNG');

    const grande = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'grande.png', { type: 'image/png' });
    fixture.componentInstance.elegirFoto(evento(grande));
    expect(fixture.componentInstance.avisoFoto()?.texto).toContain('5 MB');
  });

  it('sube una foto valida y avisa que se actualizo', () => {
    const fixture = crear(estudiante);

    fixture.componentInstance.elegirFoto(evento(new File(['x'], 'yo.png', { type: 'image/png' })));
    http.expectOne((r) => r.url.endsWith('/perfil/foto') && r.method === 'POST')
      .flush({ ...estudiante, tieneFoto: true });
    http.expectOne((r) => r.url.endsWith('/perfil/foto') && r.method === 'GET').flush(new Blob(['x']));
    fixture.detectChanges();

    expect(fixture.componentInstance.avisoFoto()?.tipo).toBe('ok');
    expect(fixture.componentInstance.fotoUrl()).toContain('blob:');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Quitar foto');
  });

  it('no envia el cambio de contrasena si el formulario es invalido o no coincide', () => {
    const fixture = crear(estudiante);

    fixture.componentInstance.cambiarPassword();
    expect(fixture.componentInstance.formPassword.invalid).toBeTrue();

    fixture.componentInstance.formPassword.setValue({ actual: 'a', nueva: 'NuevaClave2026', confirmacion: 'Otra' });
    fixture.componentInstance.cambiarPassword();
    expect(fixture.componentInstance.formPassword.hasError('noCoincide')).toBeTrue();

    fixture.componentInstance.formPassword.setValue({ actual: 'a', nueva: 'corta', confirmacion: 'corta' });
    fixture.componentInstance.cambiarPassword();
    http.expectNone((r) => r.url.endsWith('/perfil/password'));
  });

  it('cambia la contrasena, limpia el formulario y avisa', () => {
    const fixture = crear(estudiante);

    fixture.componentInstance.formPassword.setValue({
      actual: '1075000001', nueva: 'NuevaClave2026', confirmacion: 'NuevaClave2026',
    });
    fixture.componentInstance.cambiarPassword();

    const req = http.expectOne((r) => r.url.endsWith('/perfil/password'));
    expect(req.request.method).toBe('PUT');
    req.flush(null, { status: 204, statusText: 'No Content' });
    fixture.detectChanges();

    expect(fixture.componentInstance.avisoPassword()?.tipo).toBe('ok');
    expect(fixture.componentInstance.formPassword.getRawValue().nueva).toBe('');
  });

  it('muestra el mensaje del servidor si la contrasena actual es incorrecta', () => {
    const fixture = crear(estudiante);

    fixture.componentInstance.formPassword.setValue({
      actual: 'mala', nueva: 'NuevaClave2026', confirmacion: 'NuevaClave2026',
    });
    fixture.componentInstance.cambiarPassword();
    http.expectOne((r) => r.url.endsWith('/perfil/password'))
      .flush({ mensaje: 'La contraseña actual no es correcta.' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(fixture.componentInstance.avisoPassword()?.texto).toBe('La contraseña actual no es correcta.');
    expect(fixture.componentInstance.avisoPassword()?.tipo).toBe('error');
  });
});