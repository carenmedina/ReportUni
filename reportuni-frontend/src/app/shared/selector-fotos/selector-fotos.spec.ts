import { TestBed } from '@angular/core/testing';
import { SelectorFotos } from './selector-fotos';

describe('SelectorFotos', () => {
  function crear() {
    TestBed.configureTestingModule({});
    const fixture = TestBed.createComponent(SelectorFotos);
    fixture.detectChanges();
    return fixture.componentInstance;
  }
  const archivo = (nombre: string, tipo: string, bytes = 10) =>
    new File([new Uint8Array(bytes)], nombre, { type: tipo });

  it('acepta JPG y PNG', () => {
    const c = crear();
    (c as any).agregar([archivo('a.jpg', 'image/jpeg'), archivo('b.png', 'image/png')]);
    expect(c.fotos().length).toBe(2);
    expect(c.error()).toBeNull();
  });

  it('rechaza formatos no permitidos', () => {
    const c = crear();
    (c as any).agregar([archivo('a.gif', 'image/gif')]);
    expect(c.fotos().length).toBe(0);
    expect(c.error()).not.toBeNull();
  });

  it('rechaza fotos de mas de 10 MB', () => {
    const c = crear();
    (c as any).agregar([archivo('grande.jpg', 'image/jpeg', 10 * 1024 * 1024 + 1)]);
    expect(c.fotos().length).toBe(0);
    expect(c.error()).not.toBeNull();
  });

  it('no pasa de 5 fotos', () => {
    const c = crear();
    (c as any).agregar(Array.from({ length: 7 }, (_, i) => archivo(`f${i}.jpg`, 'image/jpeg')));
    expect(c.fotos().length).toBe(5);
    expect(c.error()).not.toBeNull();
  });
});