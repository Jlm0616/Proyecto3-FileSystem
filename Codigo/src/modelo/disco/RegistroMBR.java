
package modelo.disco;

/**
 * Representa el Master Boot Record (MBR) simulado
 * del disco virtual.
 *
 * Su responsabilidad es almacenar la informacion
 * de identificacion del disco y su particion.
 *
 * No realiza operaciones de lectura o escritura.
 */
public final class RegistroMBR {

    // Identificador del formato MBR propio.
    public static final int MAGIC_NUMBER = 0x4D425231; // "MBR1"

    // Version del formato.
    public static final int VERSION = 1;

    // Firma tradicional de un MBR.
    public static final int FIRMA_MBR = 0xAA55;

    // Identificador de nuestra particion virtual.
    public static final int TIPO_PARTICION = 0x7F;

    private final int magicNumber;
    private final int version;
    private final long identificadorDisco;
    private final long tamanoDiscoBytes;

    private final long inicioParticion;
    private final long tamanoParticion;

    private final int tipoParticion;
    private final int firmaMBR;

    /**
     * Construye la representacion del MBR simulado.
     *
     * @param identificadorDisco identificador unico del disco
     * @param tamanoDiscoBytes capacidad fisica del archivo .fs
     * @param inicioParticion posicion inicial de la particion
     * @param tamanoParticion tamano de la particion en bytes
     */
    public RegistroMBR(
            long identificadorDisco,
            long tamanoDiscoBytes,
            long inicioParticion,
            long tamanoParticion) {

        if (identificadorDisco <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del disco debe ser positivo.");
        }

        if (tamanoDiscoBytes <= 0) {
            throw new IllegalArgumentException(
                    "El tamano del disco debe ser positivo.");
        }

        if (inicioParticion < DistribucionDisco.TAMANO_MBR) {
            throw new IllegalArgumentException(
                    "La particion no puede superponerse con el MBR.");
        }

        if (tamanoParticion <= 0
                || inicioParticion > tamanoDiscoBytes
                || tamanoParticion > tamanoDiscoBytes - inicioParticion) {

            throw new IllegalArgumentException(
                    "La particion excede los limites del disco.");
        }

        this.magicNumber = MAGIC_NUMBER;
        this.version = VERSION;

        this.identificadorDisco = identificadorDisco;
        this.tamanoDiscoBytes = tamanoDiscoBytes;

        this.inicioParticion = inicioParticion;
        this.tamanoParticion = tamanoParticion;

        this.tipoParticion = TIPO_PARTICION;
        this.firmaMBR = FIRMA_MBR;
    }

    public int getMagicNumber() {
        return magicNumber;
    }

    public int getVersion() {
        return version;
    }

    public long getIdentificadorDisco() {
        return identificadorDisco;
    }

    public long getTamanoDiscoBytes() {
        return tamanoDiscoBytes;
    }

    public long getInicioParticion() {
        return inicioParticion;
    }

    public long getTamanoParticion() {
        return tamanoParticion;
    }

    public int getTipoParticion() {
        return tipoParticion;
    }

    public int getFirmaMBR() {
        return firmaMBR;
    }
}
