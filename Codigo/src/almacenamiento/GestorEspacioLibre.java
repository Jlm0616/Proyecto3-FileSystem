package almacenamiento;

import java.io.IOException;
import modelo.disco.Superbloque;

/**
 * Administra el espacio libre del disco virtual
 * mediante un bitmap persistente.
 *
 * Cada bit representa un bloque de datos:
 * 0 = libre
 * 1 = ocupado
 *
 * No administra archivos ni directorios.
 */
public final class GestorEspacioLibre {

    private final DiscoVirtual disco;

    private final long inicioBitmap;
    private final long cantidadBloques;
    private final int tamanoBitmap;

    public GestorEspacioLibre(DiscoVirtual disco) {

        if (disco == null) {
            throw new IllegalArgumentException(
                    "El disco virtual no puede ser nulo.");
        }

        this.disco = disco;

        Superbloque superbloque =
                disco.getSuperbloque();

        this.inicioBitmap =
                superbloque.getInicioBitmap();

        this.cantidadBloques =
                superbloque.getCantidadBloquesDatos();

        long bytesNecesarios =
                cantidadBloques / 8
                + (cantidadBloques % 8 == 0 ? 0 : 1);

        if (cantidadBloques <= 0
                || bytesNecesarios > Integer.MAX_VALUE
                || inicioBitmap < 0
                || inicioBitmap > superbloque.getInicioMetadatos()
                || bytesNecesarios
                > superbloque.getInicioMetadatos() - inicioBitmap) {

            throw new IllegalArgumentException(
                    "La region del bitmap es invalida.");
        }

        this.tamanoBitmap = (int) bytesNecesarios;
    }

    /**
     * Comprueba si un bloque esta ocupado.
     */
    public boolean estaOcupado(
            long indiceBloque) throws IOException {

        validarIndice(indiceBloque);

        synchronized (disco) {
            return consultarBit(indiceBloque);
        }
    }

    /**
     * Marca un bloque como ocupado.
     */
    public void ocuparBloque(
            long indiceBloque) throws IOException {

        validarIndice(indiceBloque);

        synchronized (disco) {

            if (consultarBit(indiceBloque)) {
                throw new IllegalStateException(
                        "El bloque ya esta ocupado: "
                        + indiceBloque);
            }

            modificarBit(indiceBloque, true);
        }
    }

    /**
     * Libera un bloque previamente ocupado.
     */
    public void liberarBloque(
            long indiceBloque) throws IOException {

        validarIndice(indiceBloque);

        synchronized (disco) {

            if (!consultarBit(indiceBloque)) {
                throw new IllegalStateException(
                        "El bloque ya esta libre: "
                        + indiceBloque);
            }

            modificarBit(indiceBloque, false);
        }
    }

    /**
     * Busca y reserva el primer bloque disponible.
     *
     * Devuelve -1 cuando el disco no tiene bloques libres.
     */
    public long reservarPrimerBloqueLibre()
            throws IOException {

        synchronized (disco) {

            byte[] bitmap =
                    disco.leer(inicioBitmap, tamanoBitmap);

            for (long indice = 0;
                    indice < cantidadBloques;
                    indice++) {

                int indiceByte = (int) (indice / 8);
                int mascara = 1 << (int) (indice % 8);

                int valor = Byte.toUnsignedInt(
                        bitmap[indiceByte]);

                if ((valor & mascara) == 0) {

                    modificarBit(indice, true);

                    return indice;
                }
            }

            return -1;
        }
    }

    /**
     * Cuenta los bloques ocupados.
     */
    public long contarBloquesOcupados()
            throws IOException {

        synchronized (disco) {

            byte[] bitmap =
                    disco.leer(inicioBitmap, tamanoBitmap);

            long ocupados = 0;

            for (int i = 0; i < bitmap.length; i++) {

                int valor = Byte.toUnsignedInt(bitmap[i]);

                // Ignorar bits sobrantes del ultimo byte.
                int bitsValidos = (int) Math.min(
                        8, cantidadBloques - (long) i * 8);

                for (int bit = 0; bit < bitsValidos; bit++) {

                    if ((valor & (1 << bit)) != 0) {
                        ocupados++;
                    }
                }
            }

            return ocupados;
        }
    }

    /**
     * Cuenta los bloques disponibles.
     */
    public long contarBloquesLibres()
            throws IOException {

        return cantidadBloques - contarBloquesOcupados();
    }

    /**
     * Consulta directamente el bit de un bloque.
     *
     * Debe ejecutarse con el bloqueo del disco adquirido.
     */
    private boolean consultarBit(
            long indiceBloque) throws IOException {

        long posicionByte =
                inicioBitmap + indiceBloque / 8;

        int posicionBit = (int) (indiceBloque % 8);

        byte[] dato = disco.leer(posicionByte, 1);

        int valor = Byte.toUnsignedInt(dato[0]);

        return (valor & (1 << posicionBit)) != 0;
    }

    /**
     * Modifica el bit correspondiente a un bloque.
     *
     * Debe ejecutarse con el bloqueo del disco adquirido.
     */
    private void modificarBit(
            long indiceBloque,
            boolean ocupado) throws IOException {

        long posicionByte =
                inicioBitmap + indiceBloque / 8;

        int posicionBit = (int) (indiceBloque % 8);

        byte[] dato = disco.leer(posicionByte, 1);

        int valor = Byte.toUnsignedInt(dato[0]);

        int mascara = 1 << posicionBit;

        if (ocupado) {
            valor |= mascara;
        } else {
            valor &= ~mascara;
        }

        disco.escribir(
                posicionByte,
                new byte[]{(byte) valor}
        );
    }

    /**
     * Verifica que el indice exista dentro
     * de la region de bloques de datos.
     */
    private void validarIndice(long indiceBloque) {

        if (indiceBloque < 0
                || indiceBloque >= cantidadBloques) {

            throw new IndexOutOfBoundsException(
                    "Bloque fuera de rango: "
                    + indiceBloque);
        }
    }

    public long getCantidadBloques() {
        return cantidadBloques;
    }

    public int getTamanoBitmap() {
        return tamanoBitmap;
    }
}