
package servicios.archivos;

import almacenamiento.GestorFCB;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import modelo.archivos.FCB;

/**
 * Servicio para resolver rutas del sistema de archivos.
 *
 * Trabaja con los FCB persistentes del disco virtual.
 *
 * No ejecuta comandos, no modifica archivos y no guarda
 * el directorio actual de una terminal.
 */
public final class ServicioRutas {

    private final GestorFCB gestorFCB;

    public ServicioRutas(GestorFCB gestorFCB) {

        if (gestorFCB == null) {
            throw new IllegalArgumentException(
                    "El gestor de FCB no puede ser nulo.");
        }

        this.gestorFCB = gestorFCB;
    }

    /**
     * Resuelve una ruta absoluta o relativa.
     *
     * @param ruta Ruta que se desea buscar.
     * @param directorioActual ID del directorio actual.
     * @return FCB correspondiente a la ruta.
     */
    public FCB resolver(
            String ruta,
            int directorioActual) throws IOException {

        if (ruta == null || ruta.isBlank()) {
            throw new IllegalArgumentException(
                    "La ruta no puede estar vacia.");
        }

        boolean absoluta = ruta.startsWith("/");

        int idActual = absoluta
                ? FCB.ID_RAIZ
                : directorioActual;

        FCB actual = obtenerFCB(idActual);

        if (!actual.esDirectorio()) {
            throw new IllegalStateException(
                    "El punto de partida no es un directorio.");
        }

        String[] componentes = ruta.split("/");

        for (String componente : componentes) {

            if (componente.isEmpty()
                    || componente.equals(".")) {
                continue;
            }

            if (componente.equals("..")) {

                if (!actual.esRaiz()) {
                    actual = obtenerFCB(actual.getIdPadre());
                }

                if (!actual.esDirectorio()) {
                    throw new IOException(
                            "El directorio padre es invalido.");
                }

                continue;
            }

            if (!actual.esDirectorio()) {
                throw new IOException(
                        "No es un directorio: "
                        + actual.getNombre());
            }

            FCB hijo = buscarHijo(
                    actual.getId(),
                    componente
            );

            if (hijo == null) {
                throw new IOException(
                        "No existe la ruta: " + ruta);
            }

            actual = hijo;
        }

        return actual;
    }

    /**
     * Busca un hijo por su nombre y directorio padre.
     *
     * Devuelve null si no existe.
     */
    public FCB buscarHijo(
            int idPadre,
            String nombre) throws IOException {

        if (nombre == null || nombre.isEmpty()) {
            throw new IllegalArgumentException(
                    "El nombre no puede estar vacio.");
        }

        FCB padre = obtenerFCB(idPadre);

        if (!padre.esDirectorio()) {
            throw new IllegalArgumentException(
                    "El padre no es un directorio.");
        }

        long capacidad =
                gestorFCB.getDistribucion().getCantidadFCB();

        FCB encontrado = null;

        for (long indice = 0; indice < capacidad; indice++) {

            FCB candidato = gestorFCB.leerFCB(indice);

            if (candidato == null) {
                continue;
            }

            if (candidato.getIdPadre() == idPadre
                    && candidato.getNombre().equals(nombre)) {

                if (encontrado != null) {
                    throw new IOException(
                            "Existen entradas duplicadas: " + nombre);
                }

                encontrado = candidato;
            }
        }

        return encontrado;
    }

    /**
     * Construye la ruta absoluta de un FCB
     * siguiendo sus referencias a directorios padre.
     */
    public String obtenerRutaAbsoluta(
            int idFCB) throws IOException {

        FCB actual = obtenerFCB(idFCB);

        if (actual.esRaiz()) {
            return "/";
        }

        List<String> nombres = new ArrayList<>();
        Set<Integer> visitados = new HashSet<>();

        long capacidad =
                gestorFCB.getDistribucion().getCantidadFCB();

        while (!actual.esRaiz()) {

            if (!visitados.add(actual.getId())) {
                throw new IOException(
                        "Se detecto un ciclo entre directorios.");
            }

            if (visitados.size() > capacidad) {
                throw new IOException(
                        "Ruta inconsistente en los metadatos.");
            }

            nombres.add(actual.getNombre());

            int idPadre = actual.getIdPadre();
            actual = obtenerFCB(idPadre);

            if (!actual.esDirectorio()) {
                throw new IOException(
                        "La ruta contiene un padre invalido.");
            }
        }

        Collections.reverse(nombres);

        return "/" + String.join("/", nombres);
    }

    /**
     * Recupera un FCB existente por su ID.
     */
    private FCB obtenerFCB(int id) throws IOException {

        if (id < 0) {
            throw new IOException(
                    "Identificador de FCB invalido.");
        }

        FCB fcb = gestorFCB.leerFCB(id);

        if (fcb == null) {
            throw new IOException(
                    "No existe el FCB con ID " + id);
        }

        return fcb;
    }
}
