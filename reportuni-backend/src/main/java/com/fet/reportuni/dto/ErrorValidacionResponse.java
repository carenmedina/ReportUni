package com.fet.reportuni.dto;

import java.util.Map;

public record ErrorValidacionResponse(String mensaje, Map<String, String> errores) {
}