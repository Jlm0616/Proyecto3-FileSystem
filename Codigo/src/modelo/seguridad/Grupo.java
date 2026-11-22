package modelo.seguridad;

/**
 * Representa un grupo del sistema de archivos virtual.
 *
 * Almacena un identificador unico y el nombre del grupo.
 *
 * No administra usuarios, permisos ni persistencia.
 */
public final class Grupo {

    private final int id;
    private final String nombre;

    public Grupo(int id, String nombre) {

        if (id < 0) {
            throw new IllegalArgumentException(
                    "El ID del grupo no puede ser negativo.");
        }

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del grupo no puede estar vacio.");
        }

        if (!nombre.matches("[a-zA-Z][a-zA-Z0-9_-]*")) {
            throw new IllegalArgumentException(
                    "El nombre del grupo contiene caracteres invalidos.");
        }

        this.id = id;
        this.nombre = nombre;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return "Grupo{"
                + "id=" + id
                + ", nombre='" + nombre + '\''
                + '}';
    }
}