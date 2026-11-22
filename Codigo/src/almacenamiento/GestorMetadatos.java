package almacenamiento;

import almacenamiento.serializacion.SerializadorGrupo;
import almacenamiento.serializacion.SerializadorUsuario;
import java.io.IOException;
import modelo.disco.DistribucionMetadatos;
import modelo.seguridad.Grupo;
import modelo.seguridad.Usuario;

/**
 * Lee y escribe registros binarios de grupos y usuarios en la zona de
 * metadatos de un DiscoVirtual. No define reglas de negocio de cuentas.
 *
 * Los registros vacios contienen todos sus bytes en cero.
 * Las operaciones individuales se sincronizan sobre la instancia del disco.
 */
public final class GestorMetadatos {

    private final DiscoVirtual disco;
    private final DistribucionMetadatos distribucion;

    public GestorMetadatos(DiscoVirtual disco) {
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

    /** Guarda un grupo solo si su registro aun esta vacio. */
    public void guardarGrupo(long indice, Grupo grupo) throws IOException {
        byte[] registro = SerializadorGrupo.serializar(grupo);
        long offset = distribucion.obtenerOffsetGrupo(indice);
        synchronized (disco) {
            exigirVacio(offset, SerializadorGrupo.TAMANO);
            disco.escribir(offset, registro);
        }
    }

    /** Devuelve null si el registro todavia no contiene un grupo. */
    public Grupo leerGrupo(long indice) throws IOException {
        long offset = distribucion.obtenerOffsetGrupo(indice);
        synchronized (disco) {
            byte[] registro = disco.leer(offset, SerializadorGrupo.TAMANO);
            return estaVacio(registro)
                    ? null : SerializadorGrupo.deserializar(registro);
        }
    }

    /** Guarda un usuario solo si su registro aun esta vacio. */
    public void guardarUsuario(long indice, Usuario usuario) throws IOException {
        byte[] registro = SerializadorUsuario.serializar(usuario);
        long offset = distribucion.obtenerOffsetUsuario(indice);
        synchronized (disco) {
            exigirVacio(offset, SerializadorUsuario.TAMANO);
            disco.escribir(offset, registro);
        }
    }

    /** Devuelve null si el registro todavia no contiene un usuario. */
    public Usuario leerUsuario(long indice) throws IOException {
        long offset = distribucion.obtenerOffsetUsuario(indice);
        synchronized (disco) {
            byte[] registro = disco.leer(offset, SerializadorUsuario.TAMANO);
            return estaVacio(registro)
                    ? null : SerializadorUsuario.deserializar(registro);
        }
    }

    public DistribucionMetadatos getDistribucion() {
        return distribucion;
    }

    private void exigirVacio(long offset, int tamano) throws IOException {
        if (!estaVacio(disco.leer(offset, tamano))) {
            throw new IllegalStateException(
                    "El registro ya esta ocupado; no se sobrescribira.");
        }
    }

    private static boolean estaVacio(byte[] registro) {
        for (byte valor : registro) {
            if (valor != 0) {
                return false;
            }
        }
        return true;
    }
}
