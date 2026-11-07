import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  CargaEstudiantesResultado,
  MAX_BYTES_CSV,
  PLANTILLA_ESTUDIANTES,
} from '../../core/models/carga-estudiantes.model';
import { EstudianteAdminService } from '../../core/services/estudiante-admin.service';

@Component({
  selector: 'app-admin-estudiantes',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-estudiantes.html',
  styleUrl: './admin-estudiantes.scss',
})
export class AdminEstudiantes {
  private readonly servicio = inject(EstudianteAdminService);

  readonly archivo = signal<File | null>(null);
  readonly cargando = signal(false);
  readonly resultado = signal<CargaEstudiantesResultado | null>(null);
  readonly error = signal<string | null>(null);

  seleccionar(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const elegido = input.files?.[0] ?? null;

    this.resultado.set(null);
    this.error.set(null);
    this.archivo.set(null);

    if (!elegido) {
      return;
    }
    if (!/\.(csv|txt)$/i.test(elegido.name)) {
      this.error.set('El archivo debe ser de tipo CSV (.csv).');
      input.value = '';
      return;
    }
    if (elegido.size > MAX_BYTES_CSV) {
      this.error.set('El archivo supera el tamaño máximo de 2 MB.');
      input.value = '';
      return;
    }
    if (elegido.size === 0) {
      this.error.set('El archivo está vacío.');
      input.value = '';
      return;
    }
    this.archivo.set(elegido);
  }

  cargar(): void {
    const archivo = this.archivo();
    if (!archivo || this.cargando()) {
      return;
    }

    this.cargando.set(true);
    this.error.set(null);
    this.resultado.set(null);

    this.servicio.cargar(archivo).subscribe({
      next: (resultado) => {
        this.resultado.set(resultado);
        this.archivo.set(null);
        this.cargando.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.servicio.interpretarError(error));
        this.cargando.set(false);
      },
    });
  }

  descargarPlantilla(): void {
    // El BOM hace que Excel abra el archivo con las tildes y la ñ correctas.
    const blob = new Blob(['﻿' + PLANTILLA_ESTUDIANTES], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = 'plantilla-estudiantes.csv';
    enlace.click();
    URL.revokeObjectURL(url);
  }

  tamano(archivo: File): string {
    return archivo.size < 1024 ? `${archivo.size} B` : `${(archivo.size / 1024).toFixed(1)} KB`;
  }
}