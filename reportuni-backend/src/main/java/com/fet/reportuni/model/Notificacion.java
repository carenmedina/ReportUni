package com.fet.reportuni.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "reporte_id", nullable = false)
    private Long reporteId;

    @Column(name = "mensaje", nullable = false, length = 255)
    private String mensaje;

    @Column(name = "leida", nullable = false)
    @Builder.Default
    private boolean leida = false;

    // true: aviso de un comentario nuevo (va al icono de comentarios, no a la campana)
    @Column(name = "es_comentario", nullable = false)
    @Builder.Default
    private boolean comentario = false;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}