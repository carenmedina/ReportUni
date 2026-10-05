package com.fet.reportuni.service;

import com.fet.reportuni.dto.MenuItem;
import com.fet.reportuni.dto.NavegacionResponse;
import com.fet.reportuni.model.Rol;
import com.fet.reportuni.model.Usuario;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NavegacionService {

    public NavegacionResponse obtenerMenu(Usuario usuario) {
        List<MenuItem> items;

        if (usuario.getRol() == Rol.ADMINISTRADOR) {
            items = List.of(
                    new MenuItem("Resumen", "/inicio", "home"),
                    new MenuItem("Reportes (gestión)", "/admin/reportes", "settings")
            );
        } else {
            items = List.of(
                    new MenuItem("Mis reportes", "/mis-reportes", "list"),
                    new MenuItem("Nuevo reporte", "/nuevo-reporte", "plus")
            );
        }

        return new NavegacionResponse(usuario.getRol().name(), items);
    }
}
