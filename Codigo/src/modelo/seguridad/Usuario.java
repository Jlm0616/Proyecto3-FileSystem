package modelo.seguridad;

import java.util.Arrays;

/**
 * Representa un usuario del sistema de archivos virtual.
 *
 * Esta clase almacena los datos del usuario.
 * No gestiona autenticacion, persistencia ni comandos.
 */
public final class Usuario {

    private final int id;
    private final String nombreUsuario;
    private final String nombreCompleto;
    private final int idGrupoPrincipal;

    private final byte[] hashContrasena;
    private final byte[] salContrasena;

    private final String directorioHome;
    private final boolean administrador;

    public Usuario(
            int id,
            String nombreUsuario,
            String nombreCompleto,
            int idGrupoPrincipal,
            byte[] hashContrasena,
            byte[] salContrasena,
            String directorioHome,
            boolean administrador) {

        if (id < 0) {
            throw new IllegalArgumentException(
                    "El ID del usuario no puede ser negativo.");
        }

        if (nombreUsuario == null
                || nombreUsuario.isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre de usuario no puede estar vacio.");
        }

        if (nombreCompleto == null
                || nombreCompleto.isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre completo no puede estar vacio.");
        }

        if (idGrupoPrincipal < 0) {
            throw new IllegalArgumentException(
                    "El ID del grupo no puede ser negativo.");
        }

        if (hashContrasena == null
                || hashContrasena.length == 0) {

            throw new IllegalArgumentException(
                    "El hash de la contrasena no es valido.");
        }

        if (salContrasena == null
                || salContrasena.length == 0) {

            throw new IllegalArgumentException(
                    "La sal de la contrasena no es valida.");
        }

        if (directorioHome == null
                || !directorioHome.startsWith("/")) {

            throw new IllegalArgumentException(
                    "El directorio HOME debe ser una ruta absoluta.");
        }

        this.id = id;
        this.nombreUsuario = nombreUsuario;
        this.nombreCompleto = nombreCompleto;
        this.idGrupoPrincipal = idGrupoPrincipal;

        // Copias defensivas para proteger los datos.
        this.hashContrasena = hashContrasena.clone();
        this.salContrasena = salContrasena.clone();

        this.directorioHome = directorioHome;
        this.administrador = administrador;
    }

    public int getId() {
        return id;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public int getIdGrupoPrincipal() {
        return idGrupoPrincipal;
    }

    public byte[] getHashContrasena() {
        return hashContrasena.clone();
    }

    public byte[] getSalContrasena() {
        return salContrasena.clone();
    }

    public String getDirectorioHome() {
        return directorioHome;
    }

    public boolean esAdministrador() {
        return administrador;
    }

    @Override
    public String toString() {
        return "Usuario{"
                + "id=" + id
                + ", nombreUsuario='" + nombreUsuario + '\''
                + ", nombreCompleto='" + nombreCompleto + '\''
                + ", idGrupoPrincipal=" + idGrupoPrincipal
                + ", directorioHome='" + directorioHome + '\''
                + ", administrador=" + administrador
                + '}';
    }
}