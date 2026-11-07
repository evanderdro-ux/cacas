package com.basefinder;

/** Caja a dibujar. Coordenadas de mundo (double); inmutable. */
public record Marker(double x0, double y0, double z0, double x1, double y1, double z1,
                     float r, float g, float b, float a, boolean outline, float thickness) {
}
