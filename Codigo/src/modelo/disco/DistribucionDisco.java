package modelo.disco;

import config.ConfiguracionDisco;

/**
 * Calcula la distribucion fisica de las regiones del disco virtual.
 *
 * Todas las posiciones representan desplazamientos en bytes
 * desde el inicio del archivo .fs.
 *
 * Esta clase no realiza operaciones de entrada/salida.
 */
public final class DistribucionDisco {

    // Espacio reservado para estructuras del volumen.
    public static final int TAMANO_MBR = 512;
    public static final int TAMANO_SUPERBLOQUE = 512;

    // Porcentaje inicial reservado para metadatos.
    private static final int PORCENTAJE_METADATOS = 10;

    private final long inicioMBR;
    private final long inicioSuperbloque;
    private final long inicioBitmap;
    private final long tamanoBitmap;

    private final long inicioMetadatos;
    private final long tamanoMetadatos;

    private final long inicioDatos;
    private final long cantidadBloquesDatos;

    private final int tamanoBloqueBytes;
    private final long tamanoDiscoBytes;

    public DistribucionDisco(ConfiguracionDisco configuracion) {

        if (configuracion == null) {
            throw new IllegalArgumentException(
                    "La configuracion no puede ser nula.");
        }

        this.tamanoDiscoBytes =
                configuracion.getTamanoDiscoBytes();

        this.tamanoBloqueBytes =
                configuracion.getTamanoBloqueBytes();

        if (tamanoDiscoBytes <= 0 || tamanoBloqueBytes <= 0) {
            throw new IllegalArgumentException(
                    "Los tamanos del disco y bloque deben ser positivos.");
        }

        // 1. MBR y superbloque.
        this.inicioMBR = 0;
        this.inicioSuperbloque = TAMANO_MBR;
        this.inicioBitmap =
                TAMANO_MBR + TAMANO_SUPERBLOQUE;

        // 2. Reservar aproximadamente 10 % para metadatos.
        // Se calcula mediante division para evitar overflow.
        long reservaMetadatos =
                (tamanoDiscoBytes / 100) * PORCENTAJE_METADATOS
                + ((tamanoDiscoBytes % 100)
                * PORCENTAJE_METADATOS) / 100;

        // 3. Calcular una estimacion de bloques.
        long espacioRestante =
                tamanoDiscoBytes - inicioBitmap - reservaMetadatos;

        if (espacioRestante <= 0) {
            throw new IllegalArgumentException(
                    "El disco no tiene espacio suficiente para las estructuras.");
        }

        // 4. Resolver la cantidad de bloques teniendo en cuenta
        // que el bitmap tambien ocupa espacio.
        long bloques = espacioRestante / tamanoBloqueBytes;

        while (bloques > 0) {

            long bytesBitmap = (bloques / 8)
                    + (bloques % 8 == 0 ? 0 : 1);

            long espacioDatos =
                    espacioRestante - bytesBitmap;

            long bloquesPosibles =
                    espacioDatos / tamanoBloqueBytes;

            if (bloquesPosibles >= bloques) {
                break;
            }

            bloques = bloquesPosibles;
        }

        if (bloques < 1) {
            throw new IllegalArgumentException(
                    "No hay espacio para bloques de datos.");
        }

        this.cantidadBloquesDatos = bloques;

        // Un bit por bloque.
        this.tamanoBitmap = (bloques / 8)
                + (bloques % 8 == 0 ? 0 : 1);

        // 5. Ubicar las regiones restantes.
        this.inicioMetadatos =
                inicioBitmap + tamanoBitmap;

        this.tamanoMetadatos = reservaMetadatos;

        this.inicioDatos =
                inicioMetadatos + tamanoMetadatos;

        // 6. Verificar que los bloques caben en el disco.
        long espacioFinal =
                tamanoDiscoBytes - inicioDatos;

        if (espacioFinal < 0
                || cantidadBloquesDatos > espacioFinal / tamanoBloqueBytes) {

            throw new IllegalArgumentException(
                    "La distribucion calculada excede el disco.");
        }
    }

    public long getInicioMBR() {
        return inicioMBR;
    }

    public long getInicioSuperbloque() {
        return inicioSuperbloque;
    }

    public long getInicioBitmap() {
        return inicioBitmap;
    }

    public long getTamanoBitmap() {
        return tamanoBitmap;
    }

    public long getInicioMetadatos() {
        return inicioMetadatos;
    }

    public long getTamanoMetadatos() {
        return tamanoMetadatos;
    }

    public long getInicioDatos() {
        return inicioDatos;
    }

    public long getCantidadBloquesDatos() {
        return cantidadBloquesDatos;
    }

    public long getTamanoDiscoBytes() {
        return tamanoDiscoBytes;
    }

    public int getTamanoBloqueBytes() {
        return tamanoBloqueBytes;
    }

    /**
     * Obtiene la posicion fisica de un bloque de datos.
     *
     * El primer bloque tiene indice 0.
     */
    public long obtenerOffsetBloque(long indiceBloque) {

        if (indiceBloque < 0
                || indiceBloque >= cantidadBloquesDatos) {

            throw new IndexOutOfBoundsException(
                    "Indice de bloque fuera de rango: " + indiceBloque);
        }

        return inicioDatos + indiceBloque * tamanoBloqueBytes;
    }

    /**
     * Calcula el espacio util total destinado a datos.
     */
    public long getCapacidadDatosBytes() {
        return cantidadBloquesDatos * tamanoBloqueBytes;
    }

    /**
     * Espacio no asignado al final del disco.
     */
    public long getEspacioSobranteBytes() {
        return tamanoDiscoBytes
                - inicioDatos
                - getCapacidadDatosBytes();
    }
}