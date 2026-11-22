package almacenamiento;

import almacenamiento.serializacion.SerializadorFCB;
import almacenamiento.serializacion.SerializadorFCB;
import java.io.IOException;
import modelo.archivos.FCB;
import modelo.disco.DistribucionMetadatos;

/**
 * Almacena y recupera registros FCB binarios en la tabla de metadatos
 * del disco virtual. No implementa directorios, permisos ni asignacion.
 *
 * Una ranura vacia contiene solo ceros. El indice de la ranura debe
 * coincidir con el identificador del FCB para evitar referencias ambiguas.
 */
public final class GestorFCB {

    private final DiscoVirtual disco;
    private final DistribucionMetadatos distribucion;

    public GestorFCB(DiscoVirtual disco) {
        if (disco == null) {
            throw new IllegalArgumentException("El disco no puede ser nulo.");
        }
        this.disco = disco;
        this.distribucion = new DistribucionMetadatos(disco.getSuperbloque());
        if (distribucion.getFinMetadatos()
                > disco.getSuperbloque().getTamanoDiscoBytes()) {
            throw new IllegalArgumentException("Los metadatos exceden el disco.");
        }
    }

    /**
     * Guarda un FCB solo si el registro correspondiente esta vacio.
     * Nunca sobrescribe un FCB existente.
     */
    public void guardarFCB(long indice, FCB fcb) throws IOException {
        if (fcb == null) {
            throw new IllegalArgumentException("El FCB no puede ser nulo.");
        }
        long offset = distribucion.obtenerOffsetFCB(indice);
        if (fcb.getId() != indice) {
            throw new IllegalArgumentException(
                    "El ID del FCB debe coincidir con su indice de tabla.");
        }
        byte[] registro = SerializadorFCB.serializar(fcb);
        synchronized (disco) {
            if (!estaVacio(disco.leer(offset, SerializadorFCB.TAMANO))) {
                throw new IllegalStateException(
                        "El registro FCB ya esta ocupado; no se sobrescribira.");
            }
            disco.escribir(offset, registro);
        }
    }

    /** Devuelve null cuando la ranura aun no contiene un FCB. */
    public FCB leerFCB(long indice) throws IOException {
        long offset = distribucion.obtenerOffsetFCB(indice);
        synchronized (disco) {
            byte[] registro = disco.leer(offset, SerializadorFCB.TAMANO);
            if (estaVacio(registro)) {
                return null;
            }
            FCB fcb = SerializadorFCB.deserializar(registro);
            if (fcb.getId() != indice) {
                throw new IOException(
                        "FCB inconsistente: ID distinto de su indice fisico.");
            }
            return fcb;
        }
    }

    public DistribucionMetadatos getDistribucion() {
        return distribucion;
    }

    private static boolean estaVacio(byte[] datos) {
        for (byte dato : datos) {
            if (dato != 0) {
                return false;
            }
        }
        return true;
    }
}