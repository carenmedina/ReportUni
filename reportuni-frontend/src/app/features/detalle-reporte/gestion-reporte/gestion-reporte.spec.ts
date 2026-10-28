import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { GestionReporte } from './gestion-reporte';

describe('GestionReporte', () => {
  let http: HttpTestingController;

  const reporte: any = {
    id: 5, codigo: '0005', tipoDano: 'ASEO', bloque: 'BIBLIOTECA', espacioEspecifico: 'Sala 1',
    prioridadEstimada: 'MEDIA', estado: 'PENDIENTE', descripcion: 'x', latitud: null, longitud: null,
    asignadoA: null, ultimaActualizacion: '2026-10-20T09:30:00', reportadoPor: 'E', fechaCreacion: '2026-10-20T09:30:00', fotos: [],
  };

  function crear() {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(GestionReporte);
    fixture.componentRef.setInput('reporte', reporte);
    fixture.detectChanges();
    return fixture;
  }

  const jpg = () => new File(['x'], 'a.jpg', { type: 'image/jpeg' });

  afterEach(() => http.verify());

  it('exige elegir el nuevo estado', () => {
    const c = crear().componentInstance;
    c.guardarEstado();
    expect(c.avisoDe('estado')?.texto).toBe('Selecciona el nuevo estado.');
  });

  it('exige explicacion para pasar a En proceso', () => {
    const c = crear().componentInstance;
    c.estadoElegido.set('EN_PROCESO');
    c.notaEstado.set('corta');
    c.guardarEstado();
    expect(c.avisoDe('estado')?.tipo).toBe('error');
    expect(c.avisoDe('estado')?.texto).toContain('entre 10 y 1000');
  });

  it('exige foto para marcar como Resuelto', () => {
    const c = crear().componentInstance;
    c.estadoElegido.set('RESUELTO');
    c.notaEstado.set('Se reparó la fuga completamente');
    c.guardarEstado();
    expect(c.avisoDe('estado')?.texto).toContain('al menos una fotografía');
  });

  it('envia el cambio de estado con nota y fotos y avisa al padre', () => {
    const fixture = crear();
    const c = fixture.componentInstance;
    let emitido = 0;
    c.cambiado.subscribe(() => emitido++);
    c.estadoElegido.set('RESUELTO');
    c.notaEstado.set('Se reparó la fuga completamente');
    c.fotosEstado.set([jpg()]);
    c.guardarEstado();

    const req = http.expectOne((r) => r.url.endsWith('/admin/reportes/5/estado'));
    expect(req.request.method).toBe('POST');
    req.flush({ id: 5 });

    expect(emitido).toBe(1);
    expect(c.avisoDe('estado')?.tipo).toBe('ok');
    expect(c.estadoElegido()).toBe('');
    expect(c.fotosEstado().length).toBe(0);
  });

  it('muestra el mensaje del servidor si el cambio falla y conserva lo escrito', () => {
    const c = crear().componentInstance;
    c.estadoElegido.set('EN_PROCESO');
    c.notaEstado.set('Se contactó al técnico');
    c.guardarEstado();
    http.expectOne((r) => r.url.endsWith('/admin/reportes/5/estado'))
      .flush({ mensaje: 'El reporte ya está en estado En proceso.' }, { status: 400, statusText: 'Bad Request' });
    expect(c.avisoDe('estado')?.texto).toBe('El reporte ya está en estado En proceso.');
    expect(c.notaEstado()).toBe('Se contactó al técnico');
  });

  it('exige elegir area y asigna el reporte', () => {
    const c = crear().componentInstance;
    c.asignar();
    expect(c.avisoDe('asignacion')?.texto).toBe('Selecciona a quién se asigna el reporte.');

    c.areaElegida.set('SOPORTE_TECNICO');
    c.asignar();
    const req = http.expectOne((r) => r.url.endsWith('/admin/reportes/5/asignacion'));
    expect(req.request.method).toBe('PUT');
    req.flush({ id: 5 });
    expect(c.avisoDe('asignacion')?.tipo).toBe('ok');
  });

  it('publica una actualizacion sin cambiar el estado', () => {
    const c = crear().componentInstance;
    c.publicarActualizacion();
    expect(c.avisoDe('actualizacion')?.tipo).toBe('error');

    c.notaActualizacion.set('El repuesto llegará en dos días hábiles');
    c.publicarActualizacion();
    http.expectOne((r) => r.url.endsWith('/admin/reportes/5/actualizaciones')).flush({ id: 5 });
    expect(c.avisoDe('actualizacion')?.tipo).toBe('ok');
    expect(c.notaActualizacion()).toBe('');
  });

  it('cambia la prioridad y devuelve el valor si falla', () => {
    const c = crear().componentInstance;
    const sel = document.createElement('select');
    ['MEDIA', 'ALTA'].forEach((v) => { const o = document.createElement('option'); o.value = v; sel.appendChild(o); });
    sel.value = 'ALTA';
    c.cambiarPrioridad(sel);
    http.expectOne((r) => r.url.endsWith('/admin/reportes/5/prioridad'))
      .flush({ mensaje: 'Error' }, { status: 400, statusText: 'Bad Request' });
    expect(sel.value).toBe('MEDIA');
    expect(c.avisoDe('prioridad')?.tipo).toBe('error');
  });
});