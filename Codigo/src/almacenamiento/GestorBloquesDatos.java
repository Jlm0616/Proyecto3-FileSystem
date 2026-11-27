package almacenamiento;

import java.io.IOException;
import java.util.Arrays;
import modelo.disco.Superbloque;

/**
 * Acceso fisico a los bloques de datos del volumen .fs.
 *
 * Los indices de bloque son relativos a la region de datos (0..N-1),
 * no offsets absolutos. Este componente NO implementa aun las estrategias
 * CONTIGUA, ENLAZADA ni INDEXADA y NO modifica FCB.
 *
 * Para proteger el bitmap, solo se permite escribir bloques reservados.
 */
public final class GestorBloquesDatos {

    private final DiscoVirtual disco;
    private final GestorEspacioLibre espacioLibre;
    private final long inicioDatos;
    private final long cantidadBloques;
    private final int tamanoBloque;

    public GestorBloquesDatos(DiscoVirtual disco, GestorEspacioLibre espacioLibre) {
        if (disco == null || espacioLibre == null) {
            throw new IllegalArgumentException("El disco y el gestor del bitmap son obligatorios.");
        }

        Superbloque sb = disco.getSuperbloque();

        this.disco = disco;
        this.espacioLibre = espacioLibre;
        this.inicioDatos = sb.getInicioDatos();
        this.cantidadBloques = sb.getCantidadBloquesDatos();
        this.tamanoBloque = sb.getTamanoBloqueBytes();

        if (cantidadBloques <= 0 || tamanoBloque <= 0
                || cantidadBloques != espacioLibre.getCantidadBloques()) {
            throw new IllegalArgumentException("Geometria del disco o bitmap incompatible.");
        }

        try {
            long fin = Math.addExact(
                    inicioDatos,
                    Math.multiplyExact(cantidadBloques, (long) tamanoBloque)
            );

            if (inicioDatos < 0 || fin > sb.getTamanoDiscoBytes()) {
                throw new IllegalArgumentException("La region de datos excede el disco.");
            }
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(
                    "Desbordamiento en la geometria del disco.", ex
            );
        }
    }

    public int getTamanoBloque() {
        return tamanoBloque;
    }

    public long getCantidadBloques() {
        return cantidadBloques;
    }

    /**
     * Traduce un indice logico de bloque
     * a su posicion fisica dentro del archivo .fs.
     */
    public long obtenerOffsetBloque(long indice) {
        validarIndice(indice);
        return inicioDatos + indice * (long) tamanoBloque;
    }

    /**
     * Lee todos los bytes de un bloque previamente reservado.
     */
    public byte[] leerBloque(long indice) throws IOException {
        synchronized (disco) {
            exigirReservado(indice);

            return disco.leer(
                    obtenerOffsetBloque(indice),
                    tamanoBloque
            );
        }
    }

    /**
     * Escribe informacion dentro de un bloque reservado.
     *
     * Si los datos son menores al tamaño del bloque,
     * el espacio restante se rellena con ceros.
     */
    public void escribirBloque(long indice, byte[] datos) throws IOException {
        if (datos == null) {
            throw new IllegalArgumentException(
                    "Los datos no pueden ser nulos."
            );
        }

        if (datos.length > tamanoBloque) {
            throw new IllegalArgumentException(
                    "Los datos exceden el tamano de bloque."
            );
        }

        synchronized (disco) {
            exigirReservado(indice);

            byte[] bloque = Arrays.copyOf(datos, tamanoBloque);

            disco.escribir(
                    obtenerOffsetBloque(indice),
                    bloque
            );
        }
    }

    private void exigirReservado(long indice) throws IOException {
        validarIndice(indice);

        if (!espacioLibre.estaOcupado(indice)) {
            throw new IOException(
                    "El bloque " + indice + " esta libre en el bitmap."
            );
        }
    }

    private void validarIndice(long indice) {
        if (indice < 0 || indice >= cantidadBloques) {
            throw new IllegalArgumentException(
                    "Indice de bloque fuera de rango: " + indice
            );
        }
    }
}