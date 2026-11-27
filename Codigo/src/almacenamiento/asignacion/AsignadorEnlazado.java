package almacenamiento.asignacion;

import almacenamiento.DiscoVirtual;
import almacenamiento.GestorBloquesDatos;
import almacenamiento.GestorEspacioLibre;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Administracion de bloques con asignacion ENLAZADA.
 *
 * Formato de cada bloque:
 *   Bytes 0..7 : indice del siguiente bloque (long BIG_ENDIAN)
 *   Bytes 8..N : contenido del archivo
 *
 * El valor -1 indica el final de la cadena.
 */
public final class AsignadorEnlazado {

    private static final long FIN_CADENA = -1L;
    private static final int BYTES_ENLACE = Long.BYTES;

    private final DiscoVirtual disco;
    private final GestorEspacioLibre bitmap;
    private final GestorBloquesDatos bloques;
    private final int capacidadDatos;

    public AsignadorEnlazado(
            DiscoVirtual disco,
            GestorEspacioLibre bitmap) {

        if (disco == null || bitmap == null) {
            throw new IllegalArgumentException(
                    "El disco y el bitmap son obligatorios."
            );
        }

        this.disco = disco;
        this.bitmap = bitmap;
        this.bloques = new GestorBloquesDatos(disco, bitmap);
        this.capacidadDatos =
                bloques.getTamanoBloque() - BYTES_ENLACE;

        if (capacidadDatos <= 0) {
            throw new IllegalArgumentException(
                    "El bloque debe tener mas de 8 bytes."
            );
        }
    }

    public int getCapacidadDatosPorBloque() {
        return capacidadDatos;
    }

    /**
     * Reserva la cantidad solicitada de bloques libres.
     *
     * No exige que sean consecutivos.
     * Devuelve el indice del primer bloque de la cadena.
     */
    public long reservar(int cantidad) throws IOException {

        if (cantidad <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad debe ser mayor que cero."
            );
        }

        synchronized (disco) {

            List<Long> reservados = new ArrayList<>();

            try {

                for (int i = 0; i < cantidad; i++) {

                    long indice = bitmap.reservarPrimerBloqueLibre();

                    if (indice < 0) {
                        throw new IOException(
                                "Espacio insuficiente para reservar "
                                + cantidad + " bloques."
                        );
                    }

                    reservados.add(indice);
                }

                for (int i = 0; i < reservados.size(); i++) {

                    long siguiente =
                            i == reservados.size() - 1
                            ? FIN_CADENA
                            : reservados.get(i + 1);

                    byte[] bloque =
                            new byte[bloques.getTamanoBloque()];

                    ByteBuffer.wrap(bloque)
                            .order(ByteOrder.BIG_ENDIAN)
                            .putLong(siguiente);

                    bloques.escribirBloque(
                            reservados.get(i),
                            bloque
                    );
                }

                return reservados.get(0);

            } catch (IOException | RuntimeException error) {

                // Intentar revertir todos los bloques reservados.
                for (long indice : reservados) {

                    try {
                        bloques.escribirBloque(
                                indice,
                                new byte[0]
                        );

                        bitmap.liberarBloque(indice);

                    } catch (IOException | RuntimeException rollback) {
                        error.addSuppressed(rollback);
                    }
                }

                throw error;
            }
        }
    }

    /**
     * Recupera los indices de una cadena siguiendo
     * el puntero que contiene cada bloque.
     */
    public List<Long> recorrer(long inicio) throws IOException {

        synchronized (disco) {

            validarIndice(inicio);

            List<Long> cadena = new ArrayList<>();
            Set<Long> visitados = new HashSet<>();

            long actual = inicio;

            while (actual != FIN_CADENA) {

                validarIndice(actual);

                if (!visitados.add(actual)) {
                    throw new IOException(
                            "Se detecto un ciclo en la cadena."
                    );
                }

                if (!bitmap.estaOcupado(actual)) {
                    throw new IOException(
                            "La cadena apunta a un bloque libre: "
                            + actual
                    );
                }

                cadena.add(actual);

                byte[] datos = bloques.leerBloque(actual);

                actual = ByteBuffer.wrap(datos)
                        .order(ByteOrder.BIG_ENDIAN)
                        .getLong();
            }

            return cadena;
        }
    }

    /**
     * Escribe el contenido repartido entre los bloques
     * previamente reservados.
     *
     * Conserva los enlaces en los primeros 8 bytes.
     */
    public void escribir(long inicio, byte[] contenido)
            throws IOException {

        if (contenido == null) {
            throw new IllegalArgumentException(
                    "El contenido no puede ser nulo."
            );
        }

        synchronized (disco) {

            List<Long> cadena = recorrer(inicio);

            long capacidadTotal =
                    (long) cadena.size() * capacidadDatos;

            if (contenido.length > capacidadTotal) {
                throw new IOException(
                        "El contenido supera la capacidad "
                        + "de la cadena reservada."
                );
            }

            int desplazamiento = 0;

            for (int i = 0; i < cadena.size(); i++) {

                long siguiente =
                        i == cadena.size() - 1
                        ? FIN_CADENA
                        : cadena.get(i + 1);

                byte[] bloque =
                        new byte[bloques.getTamanoBloque()];

                ByteBuffer.wrap(bloque)
                        .order(ByteOrder.BIG_ENDIAN)
                        .putLong(siguiente);

                int restantes =
                        contenido.length - desplazamiento;

                int cantidad = Math.min(
                        capacidadDatos,
                        restantes
                );

                if (cantidad > 0) {

                    System.arraycopy(
                            contenido,
                            desplazamiento,
                            bloque,
                            BYTES_ENLACE,
                            cantidad
                    );

                    desplazamiento += cantidad;
                }

                bloques.escribirBloque(
                        cadena.get(i),
                        bloque
                );
            }
        }
    }

    /**
     * Lee exactamente la cantidad de bytes indicada.
     *
     * El tamano real del archivo debera obtenerse
     * del FCB cuando integremos estas estrategias.
     */
    public byte[] leer(long inicio, int tamano)
            throws IOException {

        if (tamano < 0) {
            throw new IllegalArgumentException(
                    "El tamano no puede ser negativo."
            );
        }

        synchronized (disco) {

            List<Long> cadena = recorrer(inicio);

            long capacidadTotal =
                    (long) cadena.size() * capacidadDatos;

            if (tamano > capacidadTotal) {
                throw new IOException(
                        "El tamano solicitado supera "
                        + "la capacidad de la cadena."
                );
            }

            byte[] resultado = new byte[tamano];
            int desplazamiento = 0;

            for (long indice : cadena) {

                if (desplazamiento == tamano) {
                    break;
                }

                byte[] bloque = bloques.leerBloque(indice);

                int cantidad = Math.min(
                        capacidadDatos,
                        tamano - desplazamiento
                );

                System.arraycopy(
                        bloque,
                        BYTES_ENLACE,
                        resultado,
                        desplazamiento,
                        cantidad
                );

                desplazamiento += cantidad;
            }

            return resultado;
        }
    }

    /**
     * Libera una cadena completa.
     *
     * Primero valida todos los enlaces.
     * Despues limpia los bloques y actualiza el bitmap.
     */
    public void liberar(long inicio) throws IOException {

        synchronized (disco) {

            List<Long> cadena = recorrer(inicio);

            // Guardamos las copias originales para intentar
            // restaurar el estado si ocurre un error recuperable.
            List<byte[]> originales = new ArrayList<>();

            for (long indice : cadena) {
                originales.add(bloques.leerBloque(indice));
            }

            int procesados = 0;

            try {

                for (long indice : cadena) {

                    bloques.escribirBloque(
                            indice,
                            new byte[0]
                    );

                    bitmap.liberarBloque(indice);
                    procesados++;
                }

            } catch (IOException | RuntimeException error) {

                // Intentar restaurar los bloques afectados.
                for (int i = 0; i < cadena.size(); i++) {

                    long indice = cadena.get(i);

                    try {

                        if (!bitmap.estaOcupado(indice)) {
                            bitmap.ocuparBloque(indice);
                        }

                        bloques.escribirBloque(
                                indice,
                                originales.get(i)
                        );

                    } catch (IOException | RuntimeException rollback) {
                        error.addSuppressed(rollback);
                    }
                }

                throw error;
            }
        }
    }

    private void validarIndice(long indice) throws IOException {

        if (indice < 0
                || indice >= bloques.getCantidadBloques()) {

            throw new IOException(
                    "Referencia de bloque invalida: " + indice
            );
        }
    }
}