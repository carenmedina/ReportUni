import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly cargando = signal(false);
  readonly errorMensaje = signal<string | null>(null);
  readonly mostrarPassword = signal(false);

  readonly form = this.fb.group({
    correoInstitucional: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  alternarPassword(): void {
    this.mostrarPassword.update((valor) => !valor);
  }

  enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMensaje.set(null);
    this.cargando.set(true);

    const { correoInstitucional, password } = this.form.getRawValue();

    this.authService
      .login({ correoInstitucional: correoInstitucional!, password: password! })
      .subscribe({
        next: () => {
          this.cargando.set(false);
          this.router.navigate(['/inicio']);
        },
        error: (err) => {
          this.cargando.set(false);
          this.errorMensaje.set(
            err?.error?.mensaje ?? 'No fue posible iniciar sesión. Intenta nuevamente.',
          );
        },
      });
  }
}
