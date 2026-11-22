package almacenamiento.serializacion;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import modelo.seguridad.Usuario;

/**
 * Convierte usuarios a registros binarios de 512 bytes
 * y permite recuperarlos.
 *
 * No administra usuarios ni realiza operaciones
 * de lectura o escritura fisica en el disco.
 */
public final class SerializadorUsuario {

    public static final int TAMANO = 512;

    private static final int MAGIC_NUMBER = 0x55535231; // USR1
    private static final int VERSION = 1;

    private static final int MAX_USUARIO = 48;
    private static final int MAX_NOMBRE = 96;
    private static final int MAX_HOME = 224;

    private static final int TAMANO_HASH = 32;
    private static final int TAMANO_SAL = 16;

    private SerializadorUsuario() {
        // Clase utilitaria.
    }

    /**
     * Convierte un Usuario en un registro de 512 bytes.
     */
    public static byte[] serializar(Usuario usuario) {

        if (usuario == null) {
            throw new IllegalArgumentException(
                    "El usuario no puede ser nulo.");
        }

        byte[] nombreUsuario = convertirTexto(
                usuario.getNombreUsuario(), MAX_USUARIO);

        byte[] nombreCompleto = convertirTexto(
                usuario.getNombreCompleto(), MAX_NOMBRE);

        byte[] directorioHome = convertirTexto(
                usuario.getDirectorioHome(), MAX_HOME);

        byte[] hash = usuario.getHashContrasena();
        byte[] sal = usuario.getSalContrasena();

        if (hash.length != TAMANO_HASH
                || sal.length != TAMANO_SAL) {

            throw new IllegalArgumentException(
                    "El hash o la sal tienen un tamano invalido.");
        }

        ByteBuffer buffer = ByteBuffer
                .allocate(TAMANO)
                .order(ByteOrder.BIG_ENDIAN);

        // Identificacion del registro.
        buffer.putInt(MAGIC_NUMBER);
        buffer.putInt(VERSION);

        // Datos principales del usuario.
        buffer.putInt(usuario.getId());
        buffer.putInt(usuario.getIdGrupoPrincipal());

        buffer.put((byte) (
                usuario.esAdministrador() ? 1 : 0));

        // Tres bytes reservados.
        buffer.position(20);

        // Longitudes de los textos.
        buffer.putInt(nombreUsuario.length);
        buffer.putInt(nombreCompleto.length);
        buffer.putInt(directorioHome.length);

        // Textos en regiones fijas.
        escribirTexto(buffer, nombreUsuario, 32, MAX_USUARIO);
        escribirTexto(buffer, nombreCompleto, 80, MAX_NOMBRE);
        escribirTexto(buffer, directorioHome, 176, MAX_HOME);

        // Credenciales protegidas.
        buffer.position(400);
        buffer.put(hash);

        buffer.position(432);
        buffer.put(sal);

        return buffer.array();
    }

    /**
     * Reconstruye un usuario desde un registro binario.
     */
    public static Usuario deserializar(byte[] datos) {

        if (datos == null || datos.length != TAMANO) {
            throw new IllegalArgumentException(
                    "El registro debe contener 512 bytes.");
        }

        ByteBuffer buffer = ByteBuffer
                .wrap(datos)
                .order(ByteOrder.BIG_ENDIAN);

        int magic = buffer.getInt();
        int version = buffer.getInt();

        if (magic != MAGIC_NUMBER) {
            throw new IllegalArgumentException(
                    "Identificador de usuario invalido.");
        }

        if (version != VERSION) {
            throw new IllegalArgumentException(
                    "Version de usuario incompatible.");
        }

        int id = buffer.getInt();
        int idGrupo = buffer.getInt();

        int indicadorAdministrador =
                Byte.toUnsignedInt(buffer.get());

        if (indicadorAdministrador != 0
                && indicadorAdministrador != 1) {

            throw new IllegalArgumentException(
                    "Indicador de administrador invalido.");
        }

        boolean administrador =
                indicadorAdministrador == 1;

        // Leer longitudes.
        buffer.position(20);

        int longitudUsuario = buffer.getInt();
        int longitudNombre = buffer.getInt();
        int longitudHome = buffer.getInt();

        // Recuperar los textos.
        String nombreUsuario = leerTexto(
                buffer, 32, longitudUsuario, MAX_USUARIO);

        String nombreCompleto = leerTexto(
                buffer, 80, longitudNombre, MAX_NOMBRE);

        String directorioHome = leerTexto(
                buffer, 176, longitudHome, MAX_HOME);

        // Recuperar hash y sal.
        byte[] hash = new byte[TAMANO_HASH];
        byte[] sal = new byte[TAMANO_SAL];

        buffer.position(400);
        buffer.get(hash);

        buffer.position(432);
        buffer.get(sal);

        return new Usuario(
                id,
                nombreUsuario,
                nombreCompleto,
                idGrupo,
                hash,
                sal,
                directorioHome,
                administrador
        );
    }

    /**
     * Convierte un texto a UTF-8 y valida su longitud.
     */
    private static byte[] convertirTexto(
            String texto, int maximo) {

        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(
                    "El texto no puede estar vacio.");
        }

        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);

        if (bytes.length > maximo) {
            throw new IllegalArgumentException(
                    "Texto demasiado largo. Maximo: "
                    + maximo + " bytes.");
        }

        return bytes;
    }

    /**
     * Escribe texto en una region binaria fija.
     */
    private static void escribirTexto(
            ByteBuffer buffer,
            byte[] texto,
            int posicion,
            int capacidad) {

        if (texto.length > capacidad) {
            throw new IllegalArgumentException(
                    "El texto excede la region asignada.");
        }

        buffer.position(posicion);
        buffer.put(texto);
    }

    /**
     * Recupera texto desde una region binaria fija.
     */
    private static String leerTexto(
            ByteBuffer buffer,
            int posicion,
            int longitud,
            int capacidad) {

        if (longitud <= 0 || longitud > capacidad) {
            throw new IllegalArgumentException(
                    "Longitud de texto invalida.");
        }

        byte[] bytes = new byte[longitud];

        buffer.position(posicion);
        buffer.get(bytes);

        return new String(
                bytes, StandardCharsets.UTF_8);
    }
}