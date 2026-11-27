package servicios.archivos;

import almacenamiento.GestorFCB;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import modelo.archivos.FCB;

/**
 * Consulta y crea directorios mediante los FCB persistentes del volumen.
 * No interpreta comandos ni calcula rutas completas.
 */
public final class ServicioDirectorios {

    private final GestorFCB gestorFCB;

    public ServicioDirectorios(GestorFCB gestorFCB) {
        if (gestorFCB == null) {
            throw new IllegalArgumentException("El gestor de FCB no puede ser nulo.");
        }
        this.gestorFCB = gestorFCB;
    }

    /** Devuelve los hijos directos del directorio, ordenados por nombre. */
    public List<FCB> listarHijos(int idDirectorio) throws IOException {
        FCB padre = obtenerDirectorio(idDirectorio);
        long capacidad = gestorFCB.getDistribucion().getCantidadFCB();
        List<FCB> hijos = new ArrayList<>();

        for (long i = 0; i < capacidad; i++) {
            FCB candidato = gestorFCB.leerFCB(i);
            if (candidato != null && candidato.getIdPadre() == padre.getId()) {
                hijos.add(candidato);
            }
        }

        hijos.sort(Comparator.comparing(FCB::getNombre));
        return hijos;
    }

    /**
     * Crea un directorio en la primera ranura FCB libre.
     * No crea padres inexistentes; eso se implementara mas adelante.
     *
     * Nota: la comprobacion de permisos y la coordinacion entre varias
     * sesiones simultaneas se integraran en sus fases correspondientes.
     */
    public synchronized FCB crearDirectorio(
            int idPadre,
            String nombre,
            int idPropietario,
            int idGrupo,
            int permisos) throws IOException {

        obtenerDirectorio(idPadre);
        validarNombre(nombre);

        long capacidad = gestorFCB.getDistribucion().getCantidadFCB();
        long primeraLibre = -1;

        // Un solo recorrido detecta duplicados y busca una ranura libre.
        for (long i = 0; i < capacidad; i++) {
            FCB candidato = gestorFCB.leerFCB(i);
            if (candidato == null) {
                if (primeraLibre == -1 && i <= Integer.MAX_VALUE) {
                    primeraLibre = i;
                }
            } else if (candidato.getIdPadre() == idPadre
                    && candidato.getNombre().equals(nombre)) {
                throw new IOException("Ya existe una entrada llamada: " + nombre);
            }
        }

        if (primeraLibre == -1) {
            throw new IOException("La tabla de FCB esta llena.");
        }

        FCB nuevo = FCB.crearDirectorio(
                (int) primeraLibre, idPadre, nombre,
                idPropietario, idGrupo, permisos);
        gestorFCB.guardarFCB(primeraLibre, nuevo);
        return nuevo;
    }

    private FCB obtenerDirectorio(int id) throws IOException {
        if (id < 0) {
            throw new IllegalArgumentException("ID de directorio invalido.");
        }
        FCB directorio = gestorFCB.leerFCB(id);
        if (directorio == null) {
            throw new IOException("No existe el directorio con ID " + id);
        }
        if (!directorio.esDirectorio()) {
            throw new IOException("No es un directorio: " + directorio.getNombre());
        }
        return directorio;
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()
                || nombre.equals(".") || nombre.equals("..")
                || nombre.contains("/") || nombre.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Nombre de directorio invalido.");
        }
        if (nombre.getBytes(StandardCharsets.UTF_8).length
                > FCB.MAX_NOMBRE_BYTES) {
            throw new IllegalArgumentException("El nombre excede 128 bytes UTF-8.");
        }
    }
}