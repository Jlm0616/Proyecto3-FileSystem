package almacenamiento.serializacion;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import modelo.archivos.FCB;

/**
 * Transforma un FCB en un registro binario fijo de 512 bytes y viceversa.
 * No lee ni escribe el disco ni administra el arbol de directorios.
 *
 * Formato version 1, BIG_ENDIAN:
 *  0  magic (4)              4  version (4)
 *  8  id (4)                12  idPadre (4)
 * 16  tipo (4)              20  propietario (4)
 * 24  grupo (4)             28  permisos (4)
 * 32  enlaces (4)           36  longitudNombreUTF8 (4)
 * 40  tamanoBytes (8)       48  bloqueInicial (8)
 * 56  cantidadBloques (8)   64  fechaCreacion (8)
 * 72  fechaModificacion (8) 80  fechaAcceso (8)
 * 88  nombreUTF8 (128)      216 reservado (296)
 */
public final class SerializadorFCB {

    public static final int TAMANO = 512;
    private static final int MAGIC_NUMBER = 0x46434231; // FCB1
    private static final int VERSION = 1;
    private static final int TIPO_ARCHIVO = 1;
    private static final int TIPO_DIRECTORIO = 2;
    private static final int OFFSET_NOMBRE = 88;

    private SerializadorFCB() {
    }

    public static byte[] serializar(FCB fcb) {
        if (fcb == null) {
            throw new IllegalArgumentException("El FCB no puede ser nulo.");
        }

        byte[] nombre = fcb.getNombre().getBytes(StandardCharsets.UTF_8);
        if (nombre.length == 0 || nombre.length > FCB.MAX_NOMBRE_BYTES) {
            throw new IllegalArgumentException("Longitud del nombre de FCB invalida.");
        }

        ByteBuffer buffer = ByteBuffer.allocate(TAMANO)
                .order(ByteOrder.BIG_ENDIAN);

        buffer.putInt(MAGIC_NUMBER);
        buffer.putInt(VERSION);
        buffer.putInt(fcb.getId());
        buffer.putInt(fcb.getIdPadre());
        buffer.putInt(fcb.esDirectorio() ? TIPO_DIRECTORIO : TIPO_ARCHIVO);
        buffer.putInt(fcb.getIdPropietario());
        buffer.putInt(fcb.getIdGrupo());
        buffer.putInt(fcb.getPermisos());
        buffer.putInt(fcb.getCantidadEnlaces());
        buffer.putInt(nombre.length);
        buffer.putLong(fcb.getTamanoBytes());
        buffer.putLong(fcb.getBloqueInicial());
        buffer.putLong(fcb.getCantidadBloques());
        buffer.putLong(fcb.getFechaCreacion());
        buffer.putLong(fcb.getFechaModificacion());
        buffer.putLong(fcb.getFechaAcceso());
        buffer.position(OFFSET_NOMBRE);
        buffer.put(nombre);

        return buffer.array();
    }

    public static FCB deserializar(byte[] datos) {
        if (datos == null || datos.length != TAMANO) {
            throw new IllegalArgumentException(
                    "El registro FCB debe contener exactamente 512 bytes.");
        }

        ByteBuffer buffer = ByteBuffer.wrap(datos)
                .order(ByteOrder.BIG_ENDIAN);

        if (buffer.getInt() != MAGIC_NUMBER) {
            throw new IllegalArgumentException("Magic Number FCB invalido.");
        }
        if (buffer.getInt() != VERSION) {
            throw new IllegalArgumentException("Version FCB incompatible.");
        }

        int id = buffer.getInt();
        int idPadre = buffer.getInt();
        int codigoTipo = buffer.getInt();
        FCB.Tipo tipo;
        if (codigoTipo == TIPO_ARCHIVO) {
            tipo = FCB.Tipo.ARCHIVO;
        } else if (codigoTipo == TIPO_DIRECTORIO) {
            tipo = FCB.Tipo.DIRECTORIO;
        } else {
            throw new IllegalArgumentException("Tipo de FCB invalido.");
        }

        int propietario = buffer.getInt();
        int grupo = buffer.getInt();
        int permisos = buffer.getInt();
        int enlaces = buffer.getInt();
        int longitudNombre = buffer.getInt();
        long tamanoBytes = buffer.getLong();
        long bloqueInicial = buffer.getLong();
        long cantidadBloques = buffer.getLong();
        long creacion = buffer.getLong();
        long modificacion = buffer.getLong();
        long acceso = buffer.getLong();

        if (longitudNombre < 1 || longitudNombre > FCB.MAX_NOMBRE_BYTES) {
            throw new IllegalArgumentException("Longitud del nombre de FCB invalida.");
        }

        byte[] nombreBytes = new byte[longitudNombre];
        buffer.position(OFFSET_NOMBRE);
        buffer.get(nombreBytes);
        String nombre = new String(nombreBytes, StandardCharsets.UTF_8);
        // Rechazar representaciones UTF-8 invalidas (el decodificador por defecto sustituye).
        if (!java.util.Arrays.equals(nombreBytes,
                nombre.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Nombre de FCB con UTF-8 invalido.");
        }

        return new FCB(id, idPadre, nombre, tipo, propietario, grupo,
                permisos, tamanoBytes, bloqueInicial, cantidadBloques,
                creacion, modificacion, acceso, enlaces);
    }
}