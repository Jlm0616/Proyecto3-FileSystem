
package almacenamiento.serializacion;

import config.ConfiguracionDisco;
import config.ConfiguracionDisco.EstrategiaAsignacion;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import modelo.disco.DistribucionDisco;
import modelo.disco.Superbloque;

/**
 * Serializa y deserializa el superbloque.
 *
 * Formato binario fijo de 512 bytes.
 * No realiza operaciones directas sobre archivos.
 */
public final class SerializadorSuperbloque {

    public static final int TAMANO =
            DistribucionDisco.TAMANO_SUPERBLOQUE;

    private static final int MAX_NOMBRE_BYTES = 128;

    private SerializadorSuperbloque() {
        // Clase utilitaria.
    }

    /**
     * Convierte un Superbloque en 512 bytes.
     */
    public static byte[] serializar(Superbloque superbloque) {

        if (superbloque == null) {
            throw new IllegalArgumentException(
                    "El superbloque no puede ser nulo.");
        }

        byte[] nombre = superbloque.getNombreFileSystem()
                .getBytes(StandardCharsets.UTF_8);

        if (nombre.length == 0
                || nombre.length > MAX_NOMBRE_BYTES) {

            throw new IllegalArgumentException(
                    "Nombre del File System invalido o demasiado largo.");
        }

        ByteBuffer buffer = ByteBuffer
                .allocate(TAMANO)
                .order(ByteOrder.BIG_ENDIAN);

        // Encabezado.
        buffer.putInt(superbloque.getMagicNumber());
        buffer.putInt(superbloque.getVersion());

        // Nombre del File System.
        buffer.putInt(nombre.length);
        buffer.put(nombre);

        // Completar la region del nombre con ceros.
        buffer.position(12 + MAX_NOMBRE_BYTES);

        // Configuracion del volumen.
        buffer.putLong(superbloque.getTamanoDiscoBytes());
        buffer.putInt(superbloque.getTamanoBloqueBytes());

        // Estrategia representada mediante codigo fijo.
        buffer.putInt(obtenerCodigoEstrategia(
                superbloque.getEstrategiaAsignacion()));

        // Desplazamientos fisicos.
        buffer.putLong(superbloque.getInicioBitmap());
        buffer.putLong(superbloque.getInicioMetadatos());
        buffer.putLong(superbloque.getInicioDatos());

        // Cantidad de bloques de datos.
        buffer.putLong(superbloque.getCantidadBloquesDatos());

        return buffer.array();
    }

    /**
     * Reconstruye el Superbloque desde sus 512 bytes.
     */
    public static Superbloque deserializar(byte[] datos) {

        if (datos == null || datos.length != TAMANO) {
            throw new IllegalArgumentException(
                    "El superbloque debe contener exactamente "
                    + TAMANO + " bytes.");
        }

        ByteBuffer buffer = ByteBuffer
                .wrap(datos)
                .order(ByteOrder.BIG_ENDIAN);

        int magicNumber = buffer.getInt();
        int version = buffer.getInt();

        if (magicNumber != Superbloque.MAGIC_NUMBER) {
            throw new IllegalArgumentException(
                    "Magic Number del superbloque invalido.");
        }

        if (version != Superbloque.VERSION) {
            throw new IllegalArgumentException(
                    "Version del superbloque no compatible.");
        }

        // Leer nombre.
        int longitudNombre = buffer.getInt();

        if (longitudNombre <= 0
                || longitudNombre > MAX_NOMBRE_BYTES) {

            throw new IllegalArgumentException(
                    "Longitud del nombre del File System invalida.");
        }

        byte[] nombreBytes = new byte[longitudNombre];
        buffer.get(nombreBytes);

        String nombreFileSystem = new String(
                nombreBytes, StandardCharsets.UTF_8);

        // Saltar el espacio reservado para el nombre.
        buffer.position(12 + MAX_NOMBRE_BYTES);

        // Leer configuracion.
        long tamanoDisco = buffer.getLong();
        int tamanoBloque = buffer.getInt();

        int codigoEstrategia = buffer.getInt();

        EstrategiaAsignacion estrategia =
                obtenerEstrategia(codigoEstrategia);

        // Leer desplazamientos.
        long inicioBitmap = buffer.getLong();
        long inicioMetadatos = buffer.getLong();
        long inicioDatos = buffer.getLong();

        long cantidadBloques = buffer.getLong();

        if (tamanoDisco <= 0 || tamanoBloque <= 0
                || inicioBitmap < 0
                || inicioMetadatos < 0
                || inicioDatos < 0
                || cantidadBloques <= 0) {

            throw new IllegalArgumentException(
                    "Metadatos del superbloque invalidos.");
        }

        // El nombre fisico del disco no se encuentra
        // almacenado en el superbloque.
        // Se usa uno temporal para reconstruir su modelo.
        ConfiguracionDisco configuracion =
                new ConfiguracionDisco(
                        "recuperado.fs",
                        nombreFileSystem,
                        tamanoDisco,
                        tamanoBloque,
                        estrategia
                );

        return new Superbloque(
                configuracion,
                inicioBitmap,
                inicioMetadatos,
                inicioDatos,
                cantidadBloques
        );
    }

    /**
     * Codigo binario estable para cada estrategia.
     */
    private static int obtenerCodigoEstrategia(
            EstrategiaAsignacion estrategia) {

        if (estrategia == null) {
            throw new IllegalArgumentException(
                    "La estrategia no puede ser nula.");
        }

        return switch (estrategia) {
            case CONTIGUA -> 1;
            case ENLAZADA -> 2;
            case INDEXADA -> 3;
        };
    }

    /**
     * Convierte un codigo binario en su estrategia.
     */
    private static EstrategiaAsignacion obtenerEstrategia(
            int codigo) {

        return switch (codigo) {
            case 1 -> EstrategiaAsignacion.CONTIGUA;
            case 2 -> EstrategiaAsignacion.ENLAZADA;
            case 3 -> EstrategiaAsignacion.INDEXADA;

            default -> throw new IllegalArgumentException(
                    "Codigo de estrategia no reconocido: "
                    + codigo);
        };
    }
}
