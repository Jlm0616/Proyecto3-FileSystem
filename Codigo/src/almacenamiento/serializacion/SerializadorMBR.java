
package almacenamiento.serializacion;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import modelo.disco.DistribucionDisco;
import modelo.disco.RegistroMBR;

/**
 * Serializa y deserializa el MBR simulado.
 *
 * Utiliza un formato binario propio de 512 bytes.
 * No realiza operaciones directas sobre archivos.
 */
public final class SerializadorMBR {

    public static final int TAMANO =
            DistribucionDisco.TAMANO_MBR;

    private SerializadorMBR() {
        // Clase utilitaria.
    }

    /**
     * Convierte un RegistroMBR en 512 bytes.
     */
    public static byte[] serializar(RegistroMBR mbr) {

        if (mbr == null) {
            throw new IllegalArgumentException(
                    "El MBR no puede ser nulo.");
        }

        ByteBuffer buffer = ByteBuffer
                .allocate(TAMANO)
                .order(ByteOrder.BIG_ENDIAN);

        // Encabezado.
        buffer.putInt(mbr.getMagicNumber());
        buffer.putInt(mbr.getVersion());

        // Identificacion del disco.
        buffer.putLong(mbr.getIdentificadorDisco());
        buffer.putLong(mbr.getTamanoDiscoBytes());

        // Particion virtual.
        buffer.putLong(mbr.getInicioParticion());
        buffer.putLong(mbr.getTamanoParticion());
        buffer.putInt(mbr.getTipoParticion());

        // Firma al final de los 512 bytes.
        buffer.position(TAMANO - Short.BYTES);
        buffer.putShort((short) mbr.getFirmaMBR());

        return buffer.array();
    }

    /**
     * Reconstruye un RegistroMBR desde 512 bytes.
     */
    public static RegistroMBR deserializar(byte[] datos) {

        if (datos == null || datos.length != TAMANO) {
            throw new IllegalArgumentException(
                    "El MBR debe contener exactamente "
                    + TAMANO + " bytes.");
        }

        ByteBuffer buffer = ByteBuffer
                .wrap(datos)
                .order(ByteOrder.BIG_ENDIAN);

        int magicNumber = buffer.getInt();
        int version = buffer.getInt();

        long identificador = buffer.getLong();
        long tamanoDisco = buffer.getLong();

        long inicioParticion = buffer.getLong();
        long tamanoParticion = buffer.getLong();

        int tipoParticion = buffer.getInt();

        // La firma se encuentra en los ultimos 2 bytes.
        int firma = Short.toUnsignedInt(
                buffer.getShort(TAMANO - Short.BYTES)
        );

        // Validar encabezado y firma.
        if (magicNumber != RegistroMBR.MAGIC_NUMBER) {
            throw new IllegalArgumentException(
                    "Magic Number del MBR invalido.");
        }

        if (version != RegistroMBR.VERSION) {
            throw new IllegalArgumentException(
                    "Version del MBR no compatible.");
        }

        if (tipoParticion != RegistroMBR.TIPO_PARTICION) {
            throw new IllegalArgumentException(
                    "Tipo de particion no reconocido.");
        }

        if (firma != RegistroMBR.FIRMA_MBR) {
            throw new IllegalArgumentException(
                    "Firma del MBR invalida.");
        }

        // El constructor valida los limites de la particion.
        return new RegistroMBR(
                identificador,
                tamanoDisco,
                inicioParticion,
                tamanoParticion
        );
    }
}
