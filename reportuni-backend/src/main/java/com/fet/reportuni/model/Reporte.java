package com.fet.reportuni.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reportes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_dano", nullable = false, length = 30)
    private TipoDano tipoDano;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad_estimada", nullable = false, length = 10)
    private PrioridadReporte prioridadEstimada;

    @Enumerated(EnumType.STRING)
    @Column(name = "bloque", nullable = false, length = 30)
    private Bloque bloque;

    @Column(name = "espacio_especifico", nullable = false, length = 120)
    private String espacioEspecifico;

    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    @Column(name = "descripcion", nullable = false, length = 1000)
    private String descripcion;

    @Column(name = "notificar_por_correo", nullable = false)
    private boolean notificarPorCorreo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoReporte estado = EstadoReporte.PENDIENTE;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "reporte", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<ReporteFoto> fotos = new ArrayList<>();

    @PrePersist
    void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public void agregarFoto(ReporteFoto foto) {
        foto.setReporte(this);
        fotos.add(foto);
    }
}