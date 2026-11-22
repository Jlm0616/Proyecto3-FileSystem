package modelo.disco;

import config.ConfiguracionDisco;
import config.ConfiguracionDisco.EstrategiaAsignacion;

/**
 * Representa los metadatos generales del sistema de archivos.
 *
 * No realiza operaciones de lectura o escritura en disco.
 */
public final class Superbloque {

    // Identificador para reconocer nuestro sistema de archivos.
    public static final int MAGIC_NUMBER = 0x4D465331; // "MFS1"

    // Version del formato interno.
    public static final int VERSION = 1;

    private final int magicNumber;
    private final int version;

    private final String nombreFileSystem;
    private final long tamanoDiscoBytes;
    private final int tamanoBloqueBytes;
    private final EstrategiaAsignacion estrategiaAsignacion;

    // Ubicaciones de las estructuras dentro del disco.
    private final long inicioBitmap;
    private final long inicioMetadatos;
    private final long inicioDatos;

    // Cantidad total de bloques de datos.
    private final long cantidadBloquesDatos;

    public Superbloque(
            ConfiguracionDisco configuracion,
            long inicioBitmap,
            long inicioMetadatos,
            long inicioDatos,
            long cantidadBloquesDatos) {

        this.magicNumber = MAGIC_NUMBER;
        this.version = VERSION;

        this.nombreFileSystem =
                configuracion.getNombreFileSystem();

        this.tamanoDiscoBytes =
                configuracion.getTamanoDiscoBytes();

        this.tamanoBloqueBytes =
                configuracion.getTamanoBloqueBytes();

        this.estrategiaAsignacion =
                configuracion.getEstrategiaAsignacion();

        this.inicioBitmap = inicioBitmap;
        this.inicioMetadatos = inicioMetadatos;
        this.inicioDatos = inicioDatos;
        this.cantidadBloquesDatos = cantidadBloquesDatos;
    }

    public int getMagicNumber() {
        return magicNumber;
    }

    public int getVersion() {
        return version;
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

    public long getInicioBitmap() {
        return inicioBitmap;
    }

    public long getInicioMetadatos() {
        return inicioMetadatos;
    }

    public long getInicioDatos() {
        return inicioDatos;
    }

    public long getCantidadBloquesDatos() {
        return cantidadBloquesDatos;
    }
}