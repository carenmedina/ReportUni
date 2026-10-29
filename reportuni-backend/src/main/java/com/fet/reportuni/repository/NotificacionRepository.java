package com.fet.reportuni.repository;

import com.fet.reportuni.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findTop30ByUsuarioIdAndComentarioFalseOrderByFechaCreacionDescIdDesc(Long usuarioId);

    long countByUsuarioIdAndComentarioFalseAndLeidaFalse(Long usuarioId);

    List<Notificacion> findByUsuarioIdAndComentarioFalseAndLeidaFalse(Long usuarioId);

    List<Notificacion> findTop30ByUsuarioIdAndComentarioTrueOrderByFechaCreacionDescIdDesc(Long usuarioId);

    long countByUsuarioIdAndComentarioTrueAndLeidaFalse(Long usuarioId);

    List<Notificacion> findByUsuarioIdAndComentarioTrueAndLeidaFalse(Long usuarioId);

    List<Notificacion> findByUsuarioIdAndReporteIdAndComentarioTrueAndLeidaFalse(Long usuarioId, Long reporteId);

    Optional<Notificacion> findByIdAndUsuarioId(Long id, Long usuarioId);
}