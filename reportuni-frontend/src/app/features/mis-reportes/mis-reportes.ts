import { Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { REPORTES_EJEMPLO } from '../../core/models/reportes-mock.data';
import { EstadoReporte } from '../../core/models/reporte.model';

@Component({
  selector: 'app-mis-reportes',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './mis-reportes.html',
  styleUrl: './mis-reportes.scss',
})
export class MisReportes {
  readonly busqueda = signal('');
  readonly filtroEstado = signal<EstadoReporte | 'TODOS'>('TODOS');

  readonly reportes = REPORTES_EJEMPLO;

  readonly reportesFiltrados = computed(() => {
    const texto = this.busqueda().trim().toLowerCase();
    const estado = this.filtroEstado();

    return this.reportes.filter((r) => {
      const coincideEstado = estado === 'TODOS' || r.estado === estado;
      const coincideTexto =
        !texto ||
        r.id.toLowerCase().includes(texto) ||
        r.tipo.toLowerCase().includes(texto) ||
        r.ubicacion.toLowerCase().includes(texto);
      return coincideEstado && coincideTexto;
    });
  });

  actualizarBusqueda(valor: string): void {
    this.busqueda.set(valor);
  }

  actualizarFiltroEstado(valor: string): void {
    this.filtroEstado.set(valor as EstadoReporte | 'TODOS');
  }

  etiquetaEstado(estado: EstadoReporte): string {
    switch (estado) {
      case 'RESUELTO':
        return 'Resuelto';
      case 'EN_PROCESO':
        return 'En proceso';
      default:
        return 'Pendiente';
    }
  }

  etiquetaPrioridad(prioridad: string): string {
    return prioridad === 'ALTA' ? 'Alta' : prioridad === 'MEDIA' ? 'Media' : 'Baja';
  }
}
