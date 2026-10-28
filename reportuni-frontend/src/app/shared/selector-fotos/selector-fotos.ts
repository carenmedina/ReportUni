import { Component, OnDestroy, computed, effect, input, model, signal } from '@angular/core';
import { LIMITE_FOTOS, MAX_BYTES_FOTO, TIPOS_FOTO_PERMITIDOS } from '../../core/models/nuevo-reporte.models';

/**
 * Selector de fotografias reutilizable (evidencia del administrador). Mismas reglas que las fotos
 * de un reporte: JPG o PNG, maximo 10 MB y hasta 5 fotos. En celular o tableta ofrece galeria,
 * archivos y camara; en escritorio, seleccionar archivos o arrastrarlos.
 */
@Component({
  selector: 'app-selector-fotos',
  standalone: true,
  templateUrl: './selector-fotos.html',
  styleUrl: './selector-fotos.scss',
})
export class SelectorFotos implements OnDestroy {
  /** Fotos elegidas (enlace en ambos sentidos: el padre puede vaciarlas con set([])). */
  readonly fotos = model<File[]>([]);
  readonly deshabilitado = input(false);
  readonly limite = LIMITE_FOTOS;

  readonly error = signal<string | null>(null);
  readonly arrastrando = signal(false);

  readonly esMovil: boolean =
    typeof window !== 'undefined' && !!window.matchMedia && window.matchMedia('(pointer: coarse)').matches;

  private readonly urls = new Map<File, string>();

  readonly vistas = computed(() => this.fotos().map((archivo) => ({ archivo, url: this.urlDe(archivo) })));
  readonly puedeAgregar = computed(() => !this.deshabilitado() && this.fotos().length < LIMITE_FOTOS);

  constructor() {
    // Libera las vistas previas de las fotos que ya no estan (quitadas o vaciadas por el padre)
    effect(() => {
      const actuales = new Set(this.fotos());
      for (const [archivo, url] of this.urls) {
        if (!actuales.has(archivo)) {
          URL.revokeObjectURL(url);
          this.urls.delete(archivo);
        }
      }
      if (actuales.size === 0) {
        this.error.set(null);
      }
    });
  }

  alSeleccionar(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    this.agregar(Array.from(input.files ?? []));
    input.value = '';
  }

  alArrastrarSobre(evento: DragEvent): void {
    evento.preventDefault();
    this.arrastrando.set(true);
  }

  alSalirArrastre(evento: DragEvent): void {
    evento.preventDefault();
    this.arrastrando.set(false);
  }

  alSoltar(evento: DragEvent): void {
    evento.preventDefault();
    this.arrastrando.set(false);
    if (this.puedeAgregar()) {
      this.agregar(Array.from(evento.dataTransfer?.files ?? []));
    }
  }

  quitar(indice: number): void {
    this.fotos.update((lista) => lista.filter((_, i) => i !== indice));
    this.error.set(null);
  }

  private agregar(archivos: File[]): void {
    this.error.set(null);
    const mensajes: string[] = [];
    const nuevas: File[] = [];

    for (const archivo of archivos) {
      if (this.fotos().length + nuevas.length >= LIMITE_FOTOS) {
        mensajes.push(`Solo puedes adjuntar hasta ${LIMITE_FOTOS} fotos.`);
        break;
      }
      if (!TIPOS_FOTO_PERMITIDOS.includes(archivo.type)) {
        mensajes.push(`"${archivo.name}" no es JPG ni PNG.`);
        continue;
      }
      if (archivo.size > MAX_BYTES_FOTO) {
        mensajes.push(`"${archivo.name}" supera los 10 MB.`);
        continue;
      }
      nuevas.push(archivo);
    }

    if (nuevas.length > 0) {
      this.fotos.update((lista) => [...lista, ...nuevas]);
    }
    if (mensajes.length > 0) {
      this.error.set(mensajes.join(' '));
    }
  }

  private urlDe(archivo: File): string {
    let url = this.urls.get(archivo);
    if (!url) {
      url = URL.createObjectURL(archivo);
      this.urls.set(archivo, url);
    }
    return url;
  }

  ngOnDestroy(): void {
    this.urls.forEach((url) => URL.revokeObjectURL(url));
    this.urls.clear();
  }
}