package config;

/**
 * Representa la configuración de un disco virtual.
 *
 * Esta clase únicamente almacena los parámetros del disco.
 * No crea archivos físicos ni realiza operaciones de almacenamiento.
 */
public final class ConfiguracionDisco {

    public enum EstrategiaAsignacion {
        CONTIGUA,
        ENLAZADA,
        INDEXADA
    }

    private final String nombreDisco;
    private final String nombreFileSystem;
    private final long tamanoDiscoBytes;
    private final int tamanoBloqueBytes;
    private final EstrategiaAsignacion estrategiaAsignacion;

    public ConfiguracionDisco(
            String nombreDisco,
            String nombreFileSystem,
            long tamanoDiscoBytes,
            int tamanoBloqueBytes,
            EstrategiaAsignacion estrategiaAsignacion) {

        this.nombreDisco = nombreDisco;
        this.nombreFileSystem = nombreFileSystem;
        this.tamanoDiscoBytes = tamanoDiscoBytes;
        this.tamanoBloqueBytes = tamanoBloqueBytes;
        this.estrategiaAsignacion = estrategiaAsignacion;
    }

    public String getNombreDisco() {
        return nombreDisco;
    }

    public String getNombreFileSystem() {
        return nombreFileSystem;
    }

    public long getTamanoDiscoBytes() {
        return tamanoDiscoBytes;
    }

    public int getTamanoBloqueBytes() {
        return tamanoBloqueBytes;
    }

    public EstrategiaAsignacion getEstrategiaAsignacion() {
        return estrategiaAsignacion;
    }

    public long getCantidadBloques() {
        return tamanoDiscoBytes / tamanoBloqueBytes;
    }
}
