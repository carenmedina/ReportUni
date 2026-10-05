import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import {
  BLOQUES,
  LIMITE_FOTOS,
  MAX_BYTES_FOTO,
  PRIORIDADES_ESTIMADAS,
  ReporteCreado,
  TIPOS_DANO,
  TIPOS_FOTO_PERMITIDOS,
} from '../../core/models/nuevo-reporte.models';
import { ReporteService } from '../../core/services/reporte.service';
import { UbicacionService } from '../../core/services/ubicacion.service';

interface FotoSeleccionada {
  archivo: File;
  vistaPrevia: string;
}

@Component({
  selector: 'app-nuevo-reporte',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './nuevo-reporte.html',
  styleUrl: './nuevo-reporte.scss',
})
export class NuevoReporte implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly reporteService = inject(ReporteService);
  private readonly ubicacionService = inject(UbicacionService);

  readonly tiposDano = TIPOS_DANO;
  readonly prioridades = PRIORIDADES_ESTIMADAS;
  readonly bloques = BLOQUES;
  readonly limiteFotos = LIMITE_FOTOS;

  readonly formulario = this.fb.nonNullable.group({
    tipoDano: ['', Validators.required],
    prioridadEstimada: ['MEDIA', Validators.required],
    bloque: ['', Validators.required],
    espacioEspecifico: ['', [Validators.required, Validators.maxLength(120)]],
    descripcion: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]],
    notificarPorCorreo: [true],
  });

  readonly fotos = signal<FotoSeleccionada[]>([]);
  readonly errorFotos = signal<string | null>(null);
  readonly arrastrando = signal(false);

  readonly ubicacion = signal<{ latitud: number; longitud: number; precisionMetros: number } | null>(null);
  readonly obteniendoUbicacion = signal(false);
  readonly errorUbicacion = signal<string | null>(null);

  readonly enviando = signal(false);
  readonly errorGeneral = signal<string | null>(null);
  readonly erroresServidor = signal<Record<string, string>>({});
  readonly creado = signal<ReporteCreado | null>(null);

  readonly puedeAgregarFotos = computed(() => this.fotos().length < LIMITE_FOTOS);

  ngOnDestroy(): void {
    this.fotos().forEach((f) => URL.revokeObjectURL(f.vistaPrevia));
  }

  invalido(campo: string): boolean {
    const control = this.formulario.get(campo);
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  errorServidor(campo: string): string | null {
    return this.erroresServidor()[campo] ?? null;
  }

  async usarUbicacionActual(): Promise<void> {
    this.errorUbicacion.set(null);
    this.obteniendoUbicacion.set(true);
    try {
      this.ubicacion.set(await this.ubicacionService.obtenerActual());
    } catch (e) {
      this.ubicacion.set(null);
      this.errorUbicacion.set(e instanceof Error ? e.message : 'No se pudo obtener la ubicación.');
    } finally {
      this.obteniendoUbicacion.set(false);
    }
  }

  quitarUbicacion(): void {
    this.ubicacion.set(null);
    this.errorUbicacion.set(null);
  }

  alSeleccionarArchivos(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    this.agregarArchivos(Array.from(input.files ?? []));
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
    this.agregarArchivos(Array.from(evento.dataTransfer?.files ?? []));
  }

  quitarFoto(indice: number): void {
    const actuales = [...this.fotos()];
    const [quitada] = actuales.splice(indice, 1);
    if (quitada) {
      URL.revokeObjectURL(quitada.vistaPrevia);
    }
    this.fotos.set(actuales);
    this.errorFotos.set(null);
  }

  private agregarArchivos(archivos: File[]): void {
    this.errorFotos.set(null);
    const mensajes: string[] = [];
    const nuevas: FotoSeleccionada[] = [];

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
      nuevas.push({ archivo, vistaPrevia: URL.createObjectURL(archivo) });
    }

    if (nuevas.length > 0) {
      this.fotos.set([...this.fotos(), ...nuevas]);
    }
    if (mensajes.length > 0) {
      this.errorFotos.set(mensajes.join(' '));
    }
  }

  cancelar(): void {
    this.router.navigateByUrl('/mis-reportes');
  }

  irAMisReportes(): void {
    this.router.navigateByUrl('/mis-reportes');
  }

  enviar(): void {
    this.errorGeneral.set(null);
    this.erroresServidor.set({});
    this.formulario.markAllAsTouched();

    if (this.fotos().length === 0) {
      this.errorFotos.set('Adjunta al menos una fotografía del daño.');
    }
    if (this.formulario.invalid || this.fotos().length === 0 || this.enviando()) {
      return;
    }

    const valores = this.formulario.getRawValue();
    const ubic = this.ubicacion();

    this.enviando.set(true);
    this.reporteService
      .crear(
        {
          tipoDano: valores.tipoDano as never,
          prioridadEstimada: valores.prioridadEstimada as never,
          bloque: valores.bloque as never,
          espacioEspecifico: valores.espacioEspecifico.trim(),
          descripcion: valores.descripcion.trim(),
          latitud: ubic?.latitud ?? null,
          longitud: ubic?.longitud ?? null,
          notificarPorCorreo: valores.notificarPorCorreo,
        },
        this.fotos().map((f) => f.archivo),
      )
      .subscribe({
        next: (respuesta) => {
          this.enviando.set(false);
          this.creado.set(respuesta);
        },
        error: (error: HttpErrorResponse) => {
          this.enviando.set(false);
          const interpretado = this.reporteService.interpretarError(error);
          this.errorGeneral.set(interpretado.mensaje);
          this.erroresServidor.set(interpretado.errores);
        },
      });
  }
}