import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import {
  MAX_BYTES_FOTO_PERFIL,
  MAX_PASSWORD,
  MIN_PASSWORD,
  TIPOS_FOTO_PERFIL,
} from '../../core/models/perfil.model';
import { PerfilService } from '../../core/services/perfil.service';

type Aviso = { tipo: 'ok' | 'error'; texto: string };

function coincidencia(grupo: AbstractControl): ValidationErrors | null {
  const nueva = grupo.get('nueva')?.value;
  const confirmacion = grupo.get('confirmacion')?.value;
  return nueva && confirmacion && nueva !== confirmacion ? { noCoincide: true } : null;
}

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './perfil.html',
  styleUrl: './perfil.scss',
})
export class PerfilPagina implements OnInit {
  private readonly servicio = inject(PerfilService);
  private readonly fb = inject(FormBuilder).nonNullable;

  readonly perfil = this.servicio.perfil;
  readonly fotoUrl = this.servicio.fotoUrl;
  readonly cargando = signal(true);
  readonly errorCarga = signal<string | null>(null);

  readonly subiendoFoto = signal(false);
  readonly avisoFoto = signal<Aviso | null>(null);

  readonly guardandoPassword = signal(false);
  readonly avisoPassword = signal<Aviso | null>(null);

  readonly minPassword = MIN_PASSWORD;

  readonly formPassword = this.fb.group(
    {
      actual: ['', [Validators.required]],
      nueva: ['', [Validators.required, Validators.minLength(MIN_PASSWORD), Validators.maxLength(MAX_PASSWORD)]],
      confirmacion: ['', [Validators.required]],
    },
    { validators: coincidencia },
  );

  readonly esAdmin = computed(() => this.perfil()?.rol === 'ADMINISTRADOR');
  readonly iniciales = computed(() => {
    const partes = (this.perfil()?.nombreCompleto ?? '').trim().split(/\s+/).filter(Boolean);
    return partes.slice(0, 2).map((p) => p[0].toUpperCase()).join('');
  });
  readonly etiquetaRol = computed(() => (this.esAdmin() ? 'Administrador' : 'Estudiante'));

  ngOnInit(): void {
    this.servicio.obtener().subscribe({
      next: (perfil) => {
        this.cargando.set(false);
        if (perfil.tieneFoto && !this.fotoUrl()) {
          this.servicio.cargar();
        }
      },
      error: (error: HttpErrorResponse) => {
        this.errorCarga.set(this.servicio.interpretarError(error, 'foto'));
        this.cargando.set(false);
      },
    });
  }

  elegirFoto(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0] ?? null;
    this.avisoFoto.set(null);
    if (!archivo) {
      return;
    }
    input.value = '';

    if (!TIPOS_FOTO_PERFIL.includes(archivo.type)) {
      this.avisoFoto.set({ tipo: 'error', texto: 'La foto debe ser una imagen JPG o PNG.' });
      return;
    }
    if (archivo.size > MAX_BYTES_FOTO_PERFIL) {
      this.avisoFoto.set({ tipo: 'error', texto: 'La foto supera el tamaño máximo de 5 MB.' });
      return;
    }

    this.subiendoFoto.set(true);
    this.servicio.subirFoto(archivo).subscribe({
      next: () => {
        this.subiendoFoto.set(false);
        this.avisoFoto.set({ tipo: 'ok', texto: 'Tu foto de perfil se actualizó.' });
      },
      error: (error: HttpErrorResponse) => {
        this.subiendoFoto.set(false);
        this.avisoFoto.set({ tipo: 'error', texto: this.servicio.interpretarError(error, 'foto') });
      },
    });
  }

  quitarFoto(): void {
    this.avisoFoto.set(null);
    this.subiendoFoto.set(true);
    this.servicio.quitarFoto().subscribe({
      next: () => {
        this.subiendoFoto.set(false);
        this.avisoFoto.set({ tipo: 'ok', texto: 'Quitaste tu foto de perfil.' });
      },
      error: (error: HttpErrorResponse) => {
        this.subiendoFoto.set(false);
        this.avisoFoto.set({ tipo: 'error', texto: this.servicio.interpretarError(error, 'foto') });
      },
    });
  }

  cambiarPassword(): void {
    this.avisoPassword.set(null);
    if (this.formPassword.invalid || this.guardandoPassword()) {
      this.formPassword.markAllAsTouched();
      return;
    }

    this.guardandoPassword.set(true);
    this.servicio.cambiarPassword(this.formPassword.getRawValue()).subscribe({
      next: () => {
        this.guardandoPassword.set(false);
        this.formPassword.reset();
        this.avisoPassword.set({ tipo: 'ok', texto: 'Tu contraseña se cambió correctamente.' });
      },
      error: (error: HttpErrorResponse) => {
        this.guardandoPassword.set(false);
        this.avisoPassword.set({ tipo: 'error', texto: this.servicio.interpretarError(error, 'password') });
      },
    });
  }

  invalido(nombre: 'actual' | 'nueva' | 'confirmacion'): boolean {
    const control = this.formPassword.controls[nombre];
    return control.invalid && (control.touched || control.dirty);
  }

  noCoincide(): boolean {
    return this.formPassword.hasError('noCoincide') && this.formPassword.controls.confirmacion.touched;
  }
}