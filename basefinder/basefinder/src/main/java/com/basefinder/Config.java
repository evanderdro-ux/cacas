package com.basefinder;

/** Ajustes. Cambia los numeros y recompila. */
public final class Config {
    private Config() {}

    /** Radio de escaneo en chunks alrededor del jugador. */
    public static final int SCAN_RADIUS_CHUNKS = 10;
    /** Chunks escaneados por tick (sube si tu PC aguanta, baja si ves lag). */
    public static final int CHUNKS_PER_TICK = 1;
    /** Cada cuantos ticks se re-escanea un chunk (20 ticks = 1 s). */
    public static final int RESCAN_TICKS = 1200;
    /** Solo se escanea por debajo de esta altura (bases subterraneas). */
    public static final int MAX_Y = 56;

    /** Tamano de la celda de deteccion de bases (50 = 50x50). */
    public static final int AREA_SIZE = 50;
    /** Puntuacion minima de una celda para considerarla base. */
    public static final int BASE_SCORE = 20;
    /** Minimo de contenedores (cofres/barriles/shulkers...) para considerarla base. */
    public static final int BASE_MIN_CONTAINERS = 2;

    /** Distancia maxima (bloques) a la que se dibuja. */
    public static final int RENDER_RANGE = 160;
    /** Maximo de marcadores dibujados por frame. */
    public static final int MAX_MARKERS = 1500;
    /** Tope de vertices por frame (proteccion contra lag). */
    public static final int MAX_VERTICES = 300_000;
    /** Maximo de bloques "decoracion" guardados por chunk. */
    public static final int MAX_DECOR_PER_CHUNK = 300;

    /** Borde violeta de los bloques transparentes. */
    public static final float[] VIOLET = {0.62f, 0.20f, 1.00f, 1.00f};
}
