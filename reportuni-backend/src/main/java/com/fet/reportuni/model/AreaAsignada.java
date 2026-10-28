package com.fet.reportuni.model;

public enum AreaAsignada {
    SOPORTE_TECNICO("Soporte Técnico · Infraestructura"),
    MANTENIMIENTO("Mantenimiento general"),
    ELECTRICO("Mantenimiento eléctrico"),
    AUDIOVISUALES("Audiovisuales"),
    SERVICIOS_GENERALES("Servicios generales · Aseo");

    private final String etiqueta;

    AreaAsignada(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}