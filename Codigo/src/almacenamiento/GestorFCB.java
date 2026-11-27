package almacenamiento;

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

    /**
     * Actualiza un registro existente conservando su ID y posicion.
     * Se usa para cambios de nombre, directorio padre y otros metadatos.
     */
    public void actualizarFCB(long indice, FCB actualizado) throws IOException {
        if (actualizado == null || actualizado.getId() != indice) {
            throw new IllegalArgumentException("El FCB actualizado debe mantener su ID.");
        }
        long offset = distribucion.obtenerOffsetFCB(indice);
        byte[] registro = SerializadorFCB.serializar(actualizado);
        synchronized (disco) {
            byte[] anterior = disco.leer(offset, SerializadorFCB.TAMANO);
            if (estaVacio(anterior)) {
                throw new IOException("No existe el FCB que se desea actualizar.");
            }
            FCB original = SerializadorFCB.deserializar(anterior);
            if (original.getId() != indice) {
                throw new IOException("El registro actual tiene un ID inconsistente.");
            }
            disco.escribir(offset, registro);
        }
    }

    /**
     * Libera un registro FCB. No libera bloques de contenido: por seguridad
     * rechaza archivos con bloques asignados hasta disponer del gestor adecuado.
     */
    public void eliminarFCB(long indice) throws IOException {
        if (indice < 3) {
            throw new IOException("No se pueden eliminar los FCB iniciales del sistema.");
        }
        long offset = distribucion.obtenerOffsetFCB(indice);
        synchronized (disco) {
            FCB existente = leerFCB(indice);
            if (existente == null) {
                throw new IOException("No existe el FCB que se desea eliminar.");
            }
            if (existente.getCantidadBloques() != 0
                    || existente.getBloqueInicial() != FCB.SIN_BLOQUE
                    || existente.getTamanoBytes() != 0
                    || existente.getCantidadEnlaces() != 1) {
                throw new IOException("El FCB tiene contenido o enlaces que aun no se pueden liberar.");
            }
            disco.escribir(offset, new byte[SerializadorFCB.TAMANO]);
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