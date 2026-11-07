import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminEstudiantes } from './admin-estudiantes';

describe('AdminEstudiantes', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function crear() {
    const fixture = TestBed.createComponent(AdminEstudiantes);
    fixture.detectChanges();
    return fixture;
  }

  function evento(...archivos: File[]): Event {
    return { target: { files: archivos, value: 'x' } } as unknown as Event;
  }

  it('al inicio el boton de cargar esta deshabilitado', () => {
    const fixture = crear();
    const boton = (fixture.nativeElement as HTMLElement).querySelector('.ru-btn-primary') as HTMLButtonElement;
    expect(boton.disabled).toBeTrue();
  });

  it('acepta un CSV valido y habilita la carga', () => {
    const fixture = crear();
    const csv = new File(['documento;nombre;correo\n1;a;b'], 'estudiantes.csv', { type: 'text/csv' });
    fixture.componentInstance.seleccionar(evento(csv));
    fixture.detectChanges();

    expect(fixture.componentInstance.archivo()).toBe(csv);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('estudiantes.csv');
  });

  it('rechaza archivos que no son CSV y vacios', () => {
    const fixture = crear();

    fixture.componentInstance.seleccionar(evento(new File(['x'], 'foto.png')));
    expect(fixture.componentInstance.archivo()).toBeNull();
    expect(fixture.componentInstance.error()).toContain('CSV');

    fixture.componentInstance.seleccionar(evento(new File([], 'vacio.csv')));
    expect(fixture.componentInstance.error()).toContain('vacío');
  });

  it('carga el archivo y muestra el resumen con los errores por fila', () => {
    const fixture = crear();
    fixture.componentInstance.seleccionar(evento(new File(['a;b;c\n1'], 'e.csv')));
    fixture.componentInstance.cargar();

    http.expectOne((r) => r.url.endsWith('/admin/estudiantes/carga')).flush({
      filasLeidas: 3, creados: 1, actualizados: 1, rechazados: 1,
      errores: [{ fila: 4, correo: 'x@fet.edu.co', motivo: 'El correo no tiene un formato válido.' }],
    });
    fixture.detectChanges();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(fixture.componentInstance.cargando()).toBeFalse();
    expect(fixture.componentInstance.archivo()).toBeNull();
    expect(texto).toContain('Resultado de la carga');
    expect(texto).toContain('El correo no tiene un formato válido.');
  });

  it('muestra el mensaje del servidor cuando el archivo es invalido', () => {
    const fixture = crear();
    fixture.componentInstance.seleccionar(evento(new File(['a'], 'e.csv')));
    fixture.componentInstance.cargar();

    http.expectOne((r) => r.url.endsWith('/admin/estudiantes/carga'))
      .flush({ mensaje: 'La primera fila debe tener los encabezados: documento, nombre y correo.' },
        { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(fixture.componentInstance.error()).toContain('encabezados');
    expect(fixture.componentInstance.resultado()).toBeNull();
  });
});