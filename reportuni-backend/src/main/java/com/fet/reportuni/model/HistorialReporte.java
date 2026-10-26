package com.fet.reportuni.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Semana 8: registro de cada cambio de estado o prioridad que hace el administrador. */
@Entity
@Table(name = "reporte_historial")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialReporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporte_id", nullable = false)
    private Reporte reporte;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 15)
    private TipoCambio tipo;

    @Column(name = "valor_anterior", nullable = false, length = 20)
    private String valorAnterior;

    @Column(name = "valor_nuevo", nullable = false, length = 20)
    private String valorNuevo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "realizado_por_id", nullable = false)
    private Usuario realizadoPor;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    @PrePersist
    void alCrear() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
}