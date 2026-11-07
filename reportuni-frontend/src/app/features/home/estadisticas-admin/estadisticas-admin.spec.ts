import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { EstadisticasAdmin } from './estadisticas-admin';
import { Estadisticas } from '../../../core/models/estadisticas.model';

describe('EstadisticasAdmin', () => {
  let http: HttpTestingController;

  const datos: Estadisticas = {
    totalReportes: 10,
    porTipo: [
      { codigo: 'FUGA_AGUA', total: 5 },
      { codigo: 'AUDIOVISUAL', total: 3 },
      { codigo: 'ASEO', total: 2 },
      { codigo: 'DANO_ELECTRICO', total: 0 },
    ],
    porBloque: [
      { codigo: 'BLOQUE_1', total: 6 },
      { codigo: 'ZONAS_COMUNES', total: 4 },
      { codigo: 'BLOQUE_2', total: 0 },
    ],
    porPrioridad: [
      { codigo: 'ALTA', total: 2 },
      { codigo: 'MEDIA', total: 5 },
      { codigo: 'BAJA', total: 3 },
    ],
    promedioResolucionDias: 2.5,
    reportesResueltosMedidos: 4,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function crear(respuesta: Estadisticas = datos) {
    const fixture = TestBed.createComponent(EstadisticasAdmin);
    http.expectOne((r) => r.url.endsWith('/admin/estadisticas')).flush(respuesta);
    fixture.detectChanges();
    return fixture;
  }

  it('muestra el ranking de danos y de bloques con sus etiquetas reales', () => {
    const fixture = crear();
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(texto).toContain('Daños más reportados');
    expect(texto).toContain('Fuga de agua');
    expect(texto).toContain('5 · 50%');
    expect(texto).toContain('Bloques con más incidentes');
    expect(texto).toContain('Bloque 1');
    expect(texto).toContain('Zonas comunes - Exteriores');
    expect(texto).toContain('6 · 60%');
  });

  it('la barra mas larga mide 100% y las demas son proporcionales', () => {
    const fixture = crear();
    const anchos = fixture.componentInstance.rankingTipo().map((b) => b.anchoBarra);

    expect(anchos).toEqual([100, 60, 40, 0]);
  });

  it('muestra la tarjeta del tiempo promedio de resolucion', () => {
    const fixture = crear();
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(fixture.componentInstance.promedioTexto()).toBe('2,5 días');
    expect(texto).toContain('Tiempo promedio de resolución');
    expect(texto).toContain('Basado en 4 reportes resueltos');
  });

  it('sin reportes resueltos explica por que no hay promedio', () => {
    const fixture = crear({ ...datos, promedioResolucionDias: null, reportesResueltosMedidos: 0 });
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(fixture.componentInstance.promedioTexto()).toBeNull();
    expect(texto).toContain('Aún no hay reportes resueltos en este periodo.');
  });

  it('dibuja la dona de prioridad con un tramo por cada prioridad con reportes', () => {
    const fixture = crear();
    const el = fixture.nativeElement as HTMLElement;
    const segmentos = fixture.componentInstance.segmentos();

    expect(segmentos.map((s) => s.etiqueta)).toEqual(['Alta', 'Media', 'Baja']);
    expect(segmentos.map((s) => s.porcentaje)).toEqual([20, 50, 30]);
    expect(segmentos[0].desplazamiento).toBe(25);
    expect(segmentos[1].desplazamiento).toBe(5);
    expect(el.querySelectorAll('.ru-dona__svg circle').length).toBe(4); // pista + 3 tramos
    expect(el.querySelector('.ru-dona__leyenda')!.textContent).toContain('Alta');
  });

  it('sin reportes en el periodo muestra un mensaje en lugar de las graficas', () => {
    const fixture = crear({
      totalReportes: 0, porTipo: [], porBloque: [], porPrioridad: [],
      promedioResolucionDias: null, reportesResueltosMedidos: 0,
    });
    const el = fixture.nativeElement as HTMLElement;

    expect(el.textContent).toContain('No hay reportes en este periodo.');
    expect(el.querySelector('.ru-est__graficas')).toBeNull();
  });

  it('al elegir los ultimos 30 dias vuelve a pedir con desde y hasta', () => {
    const fixture = crear();

    fixture.componentInstance.elegirPreset('30');
    const req = http.expectOne((r) => r.url.endsWith('/admin/estadisticas'));
    expect(req.request.params.get('desde')).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(req.request.params.get('hasta')).toBe(fixture.componentInstance.hoy);
    req.flush(datos);

    expect(fixture.componentInstance.preset()).toBe('30');
  });

  it('volver a Todo quita el filtro de fechas', () => {
    const fixture = crear();
    fixture.componentInstance.elegirPreset('90');
    http.expectOne((r) => r.url.endsWith('/admin/estadisticas')).flush(datos);

    fixture.componentInstance.elegirPreset('TODO');
    const req = http.expectOne((r) => r.url.endsWith('/admin/estadisticas'));
    expect(req.request.params.keys().length).toBe(0);
    req.flush(datos);
  });

  it('un rango personalizado envia las fechas elegidas', () => {
    const fixture = crear();

    fixture.componentInstance.cambiarFecha('desde', '2026-10-01');
    http.expectOne((r) => r.url.endsWith('/admin/estadisticas') && r.params.get('desde') === '2026-10-01').flush(datos);

    fixture.componentInstance.cambiarFecha('hasta', '2026-10-31');
    const req = http.expectOne((r) => r.url.endsWith('/admin/estadisticas'));
    expect(req.request.params.get('desde')).toBe('2026-10-01');
    expect(req.request.params.get('hasta')).toBe('2026-10-31');
    req.flush(datos);
    expect(fixture.componentInstance.preset()).toBe('PERSONALIZADO');
  });

  it('muestra el mensaje del servidor si el rango es invalido y permite reintentar', () => {
    const fixture = crear();

    fixture.componentInstance.cambiarFecha('desde', '2026-12-01');
    http.expectOne((r) => r.url.endsWith('/admin/estadisticas'))
      .flush({ mensaje: 'La fecha inicial no puede ser posterior a la fecha final.' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(fixture.componentInstance.error()).toContain('posterior');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Reintentar');
  });
});