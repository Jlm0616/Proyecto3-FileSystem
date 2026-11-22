
package modelo.archivos;

import java.nio.charset.StandardCharsets;

/**
 * Representa el bloque de control de un archivo o directorio
 * dentro del sistema de archivos virtual.
 *
 * Esta clase almacena metadatos, no contenido de archivos.
 */
public final class FCB {

    public enum Tipo {
        ARCHIVO,
        DIRECTORIO
    }

    public static final int ID_RAIZ = 0;
    public static final int SIN_PADRE = -1;
    public static final long SIN_BLOQUE = -1L;
    public static final int MAX_NOMBRE_BYTES = 128;

    private final int id;
    private final int idPadre;
    private final String nombre;
    private final Tipo tipo;

    private final int idPropietario;
    private final int idGrupo;
    private final int permisos;

    private final long tamanoBytes;
    private final long bloqueInicial;
    private final long cantidadBloques;

    private final long fechaCreacion;
    private final long fechaModificacion;
    private final long fechaAcceso;

    private final int cantidadEnlaces;

    public FCB(
            int id,
            int idPadre,
            String nombre,
            Tipo tipo,
            int idPropietario,
            int idGrupo,
            int permisos,
            long tamanoBytes,
            long bloqueInicial,
            long cantidadBloques,
            long fechaCreacion,
            long fechaModificacion,
            long fechaAcceso,
            int cantidadEnlaces) {

        if (id < 0) {
            throw new IllegalArgumentException(
                    "El ID del FCB no puede ser negativo.");
        }

        if (idPadre < SIN_PADRE) {
            throw new IllegalArgumentException(
                    "El ID del directorio padre es invalido.");
        }

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre no puede estar vacio.");
        }

        if (nombre.getBytes(StandardCharsets.UTF_8).length
                > MAX_NOMBRE_BYTES) {

            throw new IllegalArgumentException(
                    "El nombre excede 128 bytes.");
        }

        if (tipo == null) {
            throw new IllegalArgumentException(
                    "El tipo de FCB es obligatorio.");
        }

        if (idPropietario < 0 || idGrupo < 0) {
            throw new IllegalArgumentException(
                    "El propietario o grupo es invalido.");
        }

        if (permisos < 0 || permisos > 0777) {
            throw new IllegalArgumentException(
                    "Los permisos deben estar entre 000 y 777.");
        }

        if (tamanoBytes < 0 || cantidadBloques < 0) {
            throw new IllegalArgumentException(
                    "El tamano o la cantidad de bloques es invalida.");
        }

        if (cantidadBloques == 0 && bloqueInicial != SIN_BLOQUE) {
            throw new IllegalArgumentException(
                    "Un FCB sin bloques debe usar SIN_BLOQUE.");
        }

        if (cantidadBloques > 0 && bloqueInicial < 0) {
            throw new IllegalArgumentException(
                    "Un FCB con bloques necesita una referencia valida.");
        }

        if (fechaCreacion < 0 || fechaModificacion < 0
                || fechaAcceso < 0) {

            throw new IllegalArgumentException(
                    "Las fechas no pueden ser negativas.");
        }

        if (cantidadEnlaces < 1) {
            throw new IllegalArgumentException(
                    "El FCB debe tener al menos un enlace.");
        }

        if (id == ID_RAIZ) {

            if (idPadre != SIN_PADRE
                    || !nombre.equals("/")
                    || tipo != Tipo.DIRECTORIO) {

                throw new IllegalArgumentException(
                        "El FCB raiz debe representar /.");
            }

        } else {

            if (idPadre < 0 || idPadre == id) {
                throw new IllegalArgumentException(
                        "El FCB necesita un directorio padre valido.");
            }

            if (nombre.equals(".")
                    || nombre.equals("..")
                    || nombre.contains("/")
                    || nombre.indexOf('\0') >= 0) {

                throw new IllegalArgumentException(
                        "El nombre contiene caracteres invalidos.");
            }
        }

        this.id = id;
        this.idPadre = idPadre;
        this.nombre = nombre;
        this.tipo = tipo;

        this.idPropietario = idPropietario;
        this.idGrupo = idGrupo;
        this.permisos = permisos;

        this.tamanoBytes = tamanoBytes;
        this.bloqueInicial = bloqueInicial;
        this.cantidadBloques = cantidadBloques;

        this.fechaCreacion = fechaCreacion;
        this.fechaModificacion = fechaModificacion;
        this.fechaAcceso = fechaAcceso;

        this.cantidadEnlaces = cantidadEnlaces;
    }

    /**
     * Crea el directorio raiz del sistema.
     */
    public static FCB crearRaiz(
            int idPropietario,
            int idGrupo) {

        long ahora = System.currentTimeMillis();

        return new FCB(
                ID_RAIZ,
                SIN_PADRE,
                "/",
                Tipo.DIRECTORIO,
                idPropietario,
                idGrupo,
                0755,
                0,
                SIN_BLOQUE,
                0,
                ahora,
                ahora,
                ahora,
                1
        );
    }

    /**
     * Crea un directorio inicialmente vacio.
     */
    public static FCB crearDirectorio(
            int id,
            int idPadre,
            String nombre,
            int idPropietario,
            int idGrupo,
            int permisos) {

        long ahora = System.currentTimeMillis();

        return new FCB(
                id,
                idPadre,
                nombre,
                Tipo.DIRECTORIO,
                idPropietario,
                idGrupo,
                permisos,
                0,
                SIN_BLOQUE,
                0,
                ahora,
                ahora,
                ahora,
                1
        );
    }

    /**
     * Crea un archivo inicialmente vacio.
     */
    public static FCB crearArchivo(
            int id,
            int idPadre,
            String nombre,
            int idPropietario,
            int idGrupo,
            int permisos) {

        long ahora = System.currentTimeMillis();

        return new FCB(
                id,
                idPadre,
                nombre,
                Tipo.ARCHIVO,
                idPropietario,
                idGrupo,
                permisos,
                0,
                SIN_BLOQUE,
                0,
                ahora,
                ahora,
                ahora,
                1
        );
    }

    public int getId() {
        return id;
    }

    public int getIdPadre() {
        return idPadre;
    }

    public String getNombre() {
        return nombre;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public int getIdPropietario() {
        return idPropietario;
    }

    public int getIdGrupo() {
        return idGrupo;
    }

    public int getPermisos() {
        return permisos;
    }

    public long getTamanoBytes() {
        return tamanoBytes;
    }

    public long getBloqueInicial() {
        return bloqueInicial;
    }

    public long getCantidadBloques() {
        return cantidadBloques;
    }

    public long getFechaCreacion() {
        return fechaCreacion;
    }

    public long getFechaModificacion() {
        return fechaModificacion;
    }

    public long getFechaAcceso() {
        return fechaAcceso;
    }

    public int getCantidadEnlaces() {
        return cantidadEnlaces;
    }

    public boolean esDirectorio() {
        return tipo == Tipo.DIRECTORIO;
    }

    public boolean esArchivo() {
        return tipo == Tipo.ARCHIVO;
    }

    public boolean esRaiz() {
        return id == ID_RAIZ;
    }

    /**
     * Devuelve los permisos en representacion octal.
     */
    public String getPermisosOctales() {
        return String.format("%03o", permisos);
    }

    @Override
    public String toString() {

        return "FCB{"
                + "id=" + id
                + ", idPadre=" + idPadre
                + ", nombre='" + nombre + '\''
                + ", tipo=" + tipo
                + ", propietario=" + idPropietario
                + ", grupo=" + idGrupo
                + ", permisos=" + getPermisosOctales()
                + ", tamanoBytes=" + tamanoBytes
                + ", bloqueInicial=" + bloqueInicial
                + ", cantidadBloques=" + cantidadBloques
                + '}';
    }
}
