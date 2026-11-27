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
 * Administracion de archivos mediante asignacion INDEXADA.
 *
 * Formato del bloque indice:
 *
 * Bytes 0-7: cantidad de bloques de datos (long).
 * Bytes siguientes: indices de bloques (long).
 *
 * Los bloques de datos conservan toda su capacidad.
 *
 * Esta primera version utiliza un unico bloque indice.
 */
public final class AsignadorIndexado {

    private static final int BYTES_ENTERO = Long.BYTES;

    private final DiscoVirtual disco;
    private final GestorEspacioLibre bitmap;
    private final GestorBloquesDatos bloques;

    private final int tamanoBloque;
    private final int maximoBloquesDatos;

    public AsignadorIndexado(
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

        this.tamanoBloque = bloques.getTamanoBloque();

        this.maximoBloquesDatos =
                (tamanoBloque - BYTES_ENTERO) / BYTES_ENTERO;

        if (maximoBloquesDatos < 1) {
            throw new IllegalArgumentException(
                    "El bloque es demasiado pequeno "
                    + "para asignacion indexada."
            );
        }
    }

    public int getTamanoBloque() {
        return tamanoBloque;
    }

    public int getMaximoBloquesDatos() {
        return maximoBloquesDatos;
    }

    /**
     * Reserva un bloque indice y N bloques de datos.
     *
     * Devuelve el indice fisico del bloque indice.
     */
    public long reservar(int cantidad) throws IOException {

        validarCantidad(cantidad);

        synchronized (disco) {

            List<Long> reservados = new ArrayList<>();

            try {

                long bloqueIndice =
                        bitmap.reservarPrimerBloqueLibre();

                if (bloqueIndice < 0) {
                    throw new IOException(
                            "No existe espacio para el bloque indice."
                    );
                }

                reservados.add(bloqueIndice);

                List<Long> datos = new ArrayList<>();

                for (int i = 0; i < cantidad; i++) {

                    long indice =
                            bitmap.reservarPrimerBloqueLibre();

                    if (indice < 0) {
                        throw new IOException(
                                "Espacio insuficiente para "
                                + cantidad + " bloques de datos."
                        );
                    }

                    datos.add(indice);
                    reservados.add(indice);
                }

                // Inicializar los bloques de contenido.
                for (long indice : datos) {

                    bloques.escribirBloque(
                            indice,
                            new byte[0]
                    );
                }

                // Construir el bloque indice.
                byte[] contenidoIndice =
                        new byte[tamanoBloque];

                ByteBuffer buffer = ByteBuffer
                        .wrap(contenidoIndice)
                        .order(ByteOrder.BIG_ENDIAN);

                buffer.putLong(datos.size());

                for (long indice : datos) {
                    buffer.putLong(indice);
                }

                // Guardar las referencias de forma persistente.
                bloques.escribirBloque(
                        bloqueIndice,
                        contenidoIndice
                );

                return bloqueIndice;

            } catch (IOException | RuntimeException error) {

                // Intentar deshacer toda la reserva.
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
     * Recupera los bloques de datos leyendo
     * las referencias guardadas en el bloque indice.
     */
    public List<Long> obtenerBloques(long bloqueIndice)
            throws IOException {

        synchronized (disco) {

            validarBloque(bloqueIndice);

            if (!bitmap.estaOcupado(bloqueIndice)) {
                throw new IOException(
                        "El bloque indice esta libre."
                );
            }

            byte[] contenido =
                    bloques.leerBloque(bloqueIndice);

            ByteBuffer buffer = ByteBuffer
                    .wrap(contenido)
                    .order(ByteOrder.BIG_ENDIAN);

            long cantidad = buffer.getLong();

            if (cantidad < 1
                    || cantidad > maximoBloquesDatos) {

                throw new IOException(
                        "Cantidad de bloques invalida "
                        + "en el bloque indice."
                );
            }

            List<Long> resultado = new ArrayList<>();
            Set<Long> visitados = new HashSet<>();

            visitados.add(bloqueIndice);

            for (int i = 0; i < (int) cantidad; i++) {

                long indice = buffer.getLong();

                validarBloque(indice);

                if (!visitados.add(indice)) {
                    throw new IOException(
                            "Referencia de bloque duplicada: "
                            + indice
                    );
                }

                if (!bitmap.estaOcupado(indice)) {
                    throw new IOException(
                            "El indice apunta a un bloque libre: "
                            + indice
                    );
                }

                resultado.add(indice);
            }

            return resultado;
        }
    }

    /**
     * Escribe contenido en los bloques de datos
     * referenciados por el bloque indice.
     */
    public void escribir(long bloqueIndice, byte[] contenido)
            throws IOException {

        if (contenido == null) {
            throw new IllegalArgumentException(
                    "El contenido no puede ser nulo."
            );
        }

        synchronized (disco) {

            List<Long> datos = obtenerBloques(bloqueIndice);

            long capacidad =
                    (long) datos.size() * tamanoBloque;

            if (contenido.length > capacidad) {
                throw new IOException(
                        "El contenido supera la capacidad "
                        + "de los bloques asignados."
                );
            }

            int desplazamiento = 0;

            for (long indice : datos) {

                int cantidad = Math.min(
                        tamanoBloque,
                        contenido.length - desplazamiento
                );

                byte[] fragmento = new byte[cantidad];

                if (cantidad > 0) {

                    System.arraycopy(
                            contenido,
                            desplazamiento,
                            fragmento,
                            0,
                            cantidad
                    );
                }

                bloques.escribirBloque(indice, fragmento);

                desplazamiento += cantidad;
            }
        }
    }

    /**
     * Lee exactamente la cantidad de bytes solicitada.
     *
     * El tamano real lo proporcionara el FCB
     * cuando integremos esta estrategia.
     */
    public byte[] leer(long bloqueIndice, int tamano)
            throws IOException {

        if (tamano < 0) {
            throw new IllegalArgumentException(
                    "El tamano no puede ser negativo."
            );
        }

        synchronized (disco) {

            List<Long> datos = obtenerBloques(bloqueIndice);

            long capacidad =
                    (long) datos.size() * tamanoBloque;

            if (tamano > capacidad) {
                throw new IOException(
                        "La lectura supera la capacidad asignada."
                );
            }

            byte[] resultado = new byte[tamano];
            int desplazamiento = 0;

            for (long indice : datos) {

                if (desplazamiento >= tamano) {
                    break;
                }

                byte[] bloque = bloques.leerBloque(indice);

                int cantidad = Math.min(
                        tamanoBloque,
                        tamano - desplazamiento
                );

                System.arraycopy(
                        bloque,
                        0,
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
     * Libera los bloques de datos y el bloque indice.
     *
     * ADVERTENCIA: esta operacion debe invocarse
     * solamente cuando el archivo propietario
     * ya no necesite dichos bloques.
     */
    public void liberar(long bloqueIndice) throws IOException {

        synchronized (disco) {

            List<Long> datos = obtenerBloques(bloqueIndice);

            List<Long> todos = new ArrayList<>(datos);

            // El bloque indice se libera al final.
            todos.add(bloqueIndice);

            List<byte[]> originales = new ArrayList<>();

            for (long indice : todos) {
                originales.add(bloques.leerBloque(indice));
            }

            try {

                for (long indice : todos) {

                    bloques.escribirBloque(
                            indice,
                            new byte[0]
                    );

                    bitmap.liberarBloque(indice);
                }

            } catch (IOException | RuntimeException error) {

                // Intentar restaurar el estado previo.
                for (int i = 0; i < todos.size(); i++) {

                    long indice = todos.get(i);

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

    private void validarCantidad(int cantidad) {

        if (cantidad <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad de bloques debe ser positiva."
            );
        }

        if (cantidad > maximoBloquesDatos) {
            throw new IllegalArgumentException(
                    "El bloque indice permite como maximo "
                    + maximoBloquesDatos
                    + " bloques de datos."
            );
        }
    }

    private void validarBloque(long indice) throws IOException {

        if (indice < 0
                || indice >= bloques.getCantidadBloques()) {

            throw new IOException(
                    "Indice de bloque fuera de rango: "
                    + indice
            );
        }
    }
}