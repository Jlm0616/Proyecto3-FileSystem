package almacenamiento.serializacion;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import modelo.seguridad.Grupo;

/**
 * Serializa y deserializa los grupos del sistema
 * de archivos virtual.
 *
 * Cada grupo ocupa un registro binario de 128 bytes.
 *
 * No administra grupos ni realiza operaciones
 * fisicas sobre el disco.
 */
public final class SerializadorGrupo {

    public static final int TAMANO = 128;

    private static final int MAGIC_NUMBER = 0x47525031; // GRP1
    private static final int VERSION = 1;

    private static final int MAX_NOMBRE_BYTES = 64;

    private SerializadorGrupo() {
        // Clase utilitaria.
    }

    /**
     * Convierte un grupo a un registro de 128 bytes.
     */
    public static byte[] serializar(Grupo grupo) {

        if (grupo == null) {
            throw new IllegalArgumentException(
                    "El grupo no puede ser nulo.");
        }

        byte[] nombre = grupo.getNombre()
                .getBytes(StandardCharsets.UTF_8);

        if (nombre.length == 0
                || nombre.length > MAX_NOMBRE_BYTES) {

            throw new IllegalArgumentException(
                    "El nombre del grupo excede "
                    + MAX_NOMBRE_BYTES + " bytes.");
        }

        ByteBuffer buffer = ByteBuffer
                .allocate(TAMANO)
                .order(ByteOrder.BIG_ENDIAN);

        // Encabezado.
        buffer.putInt(MAGIC_NUMBER);
        buffer.putInt(VERSION);

        // Informacion del grupo.
        buffer.putInt(grupo.getId());
        buffer.putInt(nombre.length);

        // Nombre en region fija.
        buffer.position(16);
        buffer.put(nombre);

        // Los bytes restantes quedan en cero.
        return buffer.array();
    }

    /**
     * Recupera un grupo desde un registro de 128 bytes.
     */
    public static Grupo deserializar(byte[] datos) {

        if (datos == null || datos.length != TAMANO) {
            throw new IllegalArgumentException(
                    "El registro del grupo debe contener "
                    + TAMANO + " bytes.");
        }

        ByteBuffer buffer = ByteBuffer
                .wrap(datos)
                .order(ByteOrder.BIG_ENDIAN);

        int magic = buffer.getInt();
        int version = buffer.getInt();

        if (magic != MAGIC_NUMBER) {
            throw new IllegalArgumentException(
                    "Identificador del grupo invalido.");
        }

        if (version != VERSION) {
            throw new IllegalArgumentException(
                    "Version de grupo no compatible.");
        }

        int id = buffer.getInt();
        int longitudNombre = buffer.getInt();

        if (longitudNombre <= 0
                || longitudNombre > MAX_NOMBRE_BYTES) {

            throw new IllegalArgumentException(
                    "Longitud del nombre del grupo invalida.");
        }

        byte[] nombreBytes = new byte[longitudNombre];

        buffer.position(16);
        buffer.get(nombreBytes);

        String nombre = new String(
                nombreBytes,
                StandardCharsets.UTF_8
        );

        return new Grupo(id, nombre);
    }
}