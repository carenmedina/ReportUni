package com.fet.reportuni.service;

import com.fet.reportuni.dto.MenuItem;
import com.fet.reportuni.dto.NavegacionResponse;
import com.fet.reportuni.model.Rol;
import com.fet.reportuni.model.Usuario;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Define la navegacion base (menu principal) que se muestra segun el rol
 * del usuario autenticado. Corresponde al compromiso de "navegacion base"
 * del Acta 003: la estructura de pantallas es la misma para ambos roles,
 * pero el administrador ve opciones adicionales de gestion.
 */
@Service
public class NavegacionService {

    public NavegacionResponse obtenerMenu(Usuario usuario) {
        List<MenuItem> items;

        if (usuario.getRol() == Rol.ADMINISTRADOR) {
            items = List.of(
                    new MenuItem("Resumen", "/inicio", "home"),
                    new MenuItem("Mis reportes", "/mis-reportes", "list"),
                    new MenuItem("Reportes (gestión)", "/admin/reportes", "settings"),
                    new MenuItem("Nuevo reporte", "/nuevo-reporte", "plus")
            );
        } else {
            items = List.of(
                    new MenuItem("Resumen", "/inicio", "home"),
                    new MenuItem("Mis reportes", "/mis-reportes", "list"),
                    new MenuItem("Nuevo reporte", "/nuevo-reporte", "plus")
            );
        }

        return new NavegacionResponse(usuario.getRol().name(), items);
    }
}
