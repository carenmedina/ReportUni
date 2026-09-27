package com.fet.reportuni.dto;

import java.util.List;

public record NavegacionResponse(
        String rol,
        List<MenuItem> items
) {
}
