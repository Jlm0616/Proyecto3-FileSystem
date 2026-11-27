package almacenamiento.asignacion;

import almacenamiento.DiscoVirtual;
import almacenamiento.GestorEspacioLibre;
import java.io.IOException;

/**
 * Gestiona la asignacion CONTIGUA de bloques.
 *
 * Busca N bloques libres consecutivos y los reserva
 * en el bitmap persistente del disco virtual.
 *
 * Todavia no asigna bloques a un FCB.
 */
public final class AsignadorContiguo {

    private final DiscoVirtual disco;
    private final GestorEspacioLibre espacioLibre;
    private final long totalBloques;

    public AsignadorContiguo(
            DiscoVirtual disco,
            GestorEspacioLibre espacioLibre) {

        if (disco == null || espacioLibre == null) {
            throw new IllegalArgumentException(
                    "El disco y el gestor de espacio son obligatorios."
            );
        }

        this.disco = disco;
        this.espacioLibre = espacioLibre;
        this.totalBloques = espacioLibre.getCantidadBloques();

        if (totalBloques !=
                disco.getSuperbloque().getCantidadBloquesDatos()) {
            throw new IllegalArgumentException(
                    "El bitmap no coincide con el disco."
            );
        }
    }

    /**
     * Busca una secuencia de N bloques libres.
     *
     * Devuelve el indice inicial o -1 si no existe
     * espacio contiguo suficiente.
     *
     * No modifica el bitmap.
     */
    public long buscarEspacioContiguo(int cantidad)
            throws IOException {

        validarCantidad(cantidad);

        synchronized (disco) {
            return buscarEspacioInterno(cantidad);
        }
    }

    /**
     * Reserva N bloques consecutivos.
     *
     * Devuelve el indice inicial del tramo reservado.
     * Devuelve -1 si no hay un tramo suficientemente grande.
     *
     * Si falla la escritura del bitmap, intenta revertir
     * las reservas realizadas durante esta operacion.
     */
    public long reservar(int cantidad) throws IOException {

        validarCantidad(cantidad);

        synchronized (disco) {

            long inicio = buscarEspacioInterno(cantidad);

            if (inicio == -1) {
                return -1;
            }

            int reservados = 0;

            try {

                for (int i = 0; i < cantidad; i++) {

                    espacioLibre.ocuparBloque(inicio + i);
                    reservados++;
                }

                return inicio;

            } catch (IOException | RuntimeException error) {

                // Revertir los bloques que se alcanzaron
                // a reservar antes del fallo.
                for (int i = reservados - 1; i >= 0; i--) {

                    try {
                        espacioLibre.liberarBloque(inicio + i);

                    } catch (IOException | RuntimeException rollback) {
                        error.addSuppressed(rollback);
                    }
                }

                throw error;
            }
        }
    }

    /**
     * Libera un tramo consecutivo.
     *
     * IMPORTANTE: El llamador debe comprobar que
     * estos bloques pertenecen al archivo que elimina.
     * El bitmap no almacena informacion de propietarios.
     */
    public void liberar(long inicio, int cantidad)
            throws IOException {

        validarRango(inicio, cantidad);

        synchronized (disco) {

            // Comprobar todo el tramo antes de modificarlo.
            for (int i = 0; i < cantidad; i++) {

                if (!espacioLibre.estaOcupado(inicio + i)) {
                    throw new IOException(
                            "El bloque " + (inicio + i)
                            + " ya esta libre."
                    );
                }
            }

            int liberados = 0;

            try {

                for (int i = 0; i < cantidad; i++) {

                    espacioLibre.liberarBloque(inicio + i);
                    liberados++;
                }

            } catch (IOException | RuntimeException error) {

                // Intentar restaurar el bitmap si una
                // liberacion falla a mitad de la operacion.
                for (int i = liberados - 1; i >= 0; i--) {

                    try {
                        espacioLibre.ocuparBloque(inicio + i);

                    } catch (IOException | RuntimeException rollback) {
                        error.addSuppressed(rollback);
                    }
                }

                throw error;
            }
        }
    }

    /**
     * Algoritmo FIRST FIT:
     * devuelve el primer tramo libre que tenga
     * al menos la cantidad de bloques solicitada.
     *
     * Se invoca con el monitor del disco adquirido.
     */
    private long buscarEspacioInterno(int cantidad)
            throws IOException {

        long inicioSecuencia = -1;
        int consecutivos = 0;

        for (long i = 0; i < totalBloques; i++) {

            if (!espacioLibre.estaOcupado(i)) {

                if (consecutivos == 0) {
                    inicioSecuencia = i;
                }

                consecutivos++;

                if (consecutivos == cantidad) {
                    return inicioSecuencia;
                }

            } else {

                inicioSecuencia = -1;
                consecutivos = 0;
            }
        }

        return -1;
    }

    private void validarCantidad(int cantidad) {

        if (cantidad <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad de bloques debe ser mayor que cero."
            );
        }
    }

    private void validarRango(long inicio, int cantidad) {

        validarCantidad(cantidad);

        if (inicio < 0
                || inicio >= totalBloques
                || cantidad > totalBloques - inicio) {

            throw new IllegalArgumentException(
                    "El tramo solicitado esta fuera del disco."
            );
        }
    }
}