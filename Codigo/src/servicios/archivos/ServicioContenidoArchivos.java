package servicios.archivos;

import almacenamiento.DiscoVirtual;
import almacenamiento.GestorBloquesDatos;
import almacenamiento.GestorEspacioLibre;
import almacenamiento.GestorFCB;
import almacenamiento.asignacion.AsignadorContiguo;
import almacenamiento.asignacion.AsignadorEnlazado;
import almacenamiento.asignacion.AsignadorIndexado;
import java.io.IOException;
import java.util.Arrays;
import modelo.archivos.FCB;

/**
 * Gestiona el contenido real de archivos persistentes.
 *
 * Selecciona automaticamente CONTIGUA, ENLAZADA o INDEXADA
 * segun la estrategia almacenada en el superbloque.
 *
 * No implementa permisos, sesiones ni comandos de consola.
 */
public final class ServicioContenidoArchivos {

    private final DiscoVirtual disco;
    private final GestorFCB gestorFCB;
    private final GestorBloquesDatos bloques;
    private final AsignadorContiguo contiguo;
    private final AsignadorEnlazado enlazado;
    private final AsignadorIndexado indexado;
    private final String estrategia;

    public ServicioContenidoArchivos(
            DiscoVirtual disco,
            GestorFCB gestorFCB,
            GestorEspacioLibre bitmap) {

        if (disco == null || gestorFCB == null || bitmap == null) {
            throw new IllegalArgumentException(
                    "El disco, los FCB y el bitmap son obligatorios."
            );
        }

        this.disco = disco;
        this.gestorFCB = gestorFCB;
        this.bloques = new GestorBloquesDatos(disco, bitmap);

        this.contiguo = new AsignadorContiguo(disco, bitmap);
        this.enlazado = new AsignadorEnlazado(disco, bitmap);
        this.indexado = new AsignadorIndexado(disco, bitmap);

        this.estrategia = disco.getSuperbloque()
                .getEstrategiaAsignacion().name();
    }

    public String getEstrategia() {
        return estrategia;
    }

    /**
     * Lee el contenido completo de un archivo mediante su FCB.
     */
    public byte[] leer(int idFCB) throws IOException {

        synchronized (disco) {

            FCB archivo = obtenerArchivo(idFCB);

            if (archivo.getTamanoBytes() == 0) {
                return new byte[0];
            }

            if (archivo.getTamanoBytes() > Integer.MAX_VALUE) {
                throw new IOException(
                        "El archivo es demasiado grande para esta lectura."
                );
            }

            validarReferencia(archivo);

            int tamano = (int) archivo.getTamanoBytes();
            long inicio = archivo.getBloqueInicial();

            switch (estrategia) {

                case "CONTIGUA":
                    return leerContiguo(
                            inicio,
                            archivo.getCantidadBloques(),
                            tamano
                    );

                case "ENLAZADA":
                    return enlazado.leer(inicio, tamano);

                case "INDEXADA":
                    return indexado.leer(inicio, tamano);

                default:
                    throw new IOException(
                            "Estrategia desconocida: " + estrategia
                    );
            }
        }
    }

    /**
     * Reemplaza todo el contenido de un archivo.
     *
     * Primero reserva y escribe los bloques nuevos.
     * Luego actualiza el FCB.
     * Finalmente libera los bloques antiguos.
     *
     * Asi, un fallo durante la escritura no destruye
     * inmediatamente el contenido anterior.
     */
    public void escribir(int idFCB, byte[] contenido)
            throws IOException {

        if (contenido == null) {
            throw new IllegalArgumentException(
                    "El contenido no puede ser nulo."
            );
        }

        synchronized (disco) {

            FCB anterior = obtenerArchivo(idFCB);

            validarReferencia(anterior);

            if (anterior.getCantidadEnlaces() != 1) {
                throw new IOException(
                        "La escritura de archivos con varios enlaces "
                        + "se implementara posteriormente."
                );
            }

            long nuevoInicio = FCB.SIN_BLOQUE;
            long nuevosBloques = 0;

            boolean reservaNueva = false;
            boolean fcbActualizado = false;

            try {

                if (contenido.length > 0) {

                    int cantidad = calcularBloques(
                            contenido.length,
                            capacidadUtil()
                    );

                    nuevoInicio = reservar(cantidad);

                    nuevosBloques = estrategia.equals("INDEXADA")
                            ? cantidad + 1L
                            : cantidad;

                    reservaNueva = true;

                    escribirEnBloques(
                            nuevoInicio,
                            nuevosBloques,
                            contenido
                    );
                }

                long ahora = System.currentTimeMillis();

                FCB actualizado = new FCB(
                        anterior.getId(),
                        anterior.getIdPadre(),
                        anterior.getNombre(),
                        anterior.getTipo(),
                        anterior.getIdPropietario(),
                        anterior.getIdGrupo(),
                        anterior.getPermisos(),
                        contenido.length,
                        nuevoInicio,
                        nuevosBloques,
                        anterior.getFechaCreacion(),
                        ahora,
                        anterior.getFechaAcceso(),
                        anterior.getCantidadEnlaces()
                );

                gestorFCB.actualizarFCB(
                        anterior.getId(),
                        actualizado
                );

                fcbActualizado = true;

            } catch (IOException | RuntimeException error) {

                // Solo liberar la nueva reserva si el FCB
                // todavia no fue actualizado.
                if (!fcbActualizado && reservaNueva) {

                    try {
                        liberarReserva(
                                nuevoInicio,
                                nuevosBloques
                        );
                    } catch (IOException | RuntimeException rollback) {
                        error.addSuppressed(rollback);
                    }
                }

                throw error;
            }

            // Desde este punto, el FCB referencia
            // correctamente el contenido nuevo.
            // Liberamos la reserva anterior.
            if (anterior.getCantidadBloques() > 0) {

                try {
                    liberarReserva(
                            anterior.getBloqueInicial(),
                            anterior.getCantidadBloques()
                    );

                } catch (IOException error) {
                    throw new IOException(
                            "El contenido nuevo fue guardado, "
                            + "pero no se pudieron liberar "
                            + "todos los bloques anteriores.",
                            error
                    );
                }
            }
        }
    }

    /**
     * Deja un archivo completamente vacio.
     */
    public void vaciar(int idFCB) throws IOException {
        escribir(idFCB, new byte[0]);
    }

    private FCB obtenerArchivo(int idFCB) throws IOException {

        FCB fcb = gestorFCB.leerFCB(idFCB);

        if (fcb == null) {
            throw new IOException(
                    "No existe el archivo con FCB " + idFCB
            );
        }

        if (!fcb.esArchivo()) {
            throw new IOException(
                    "La entrada es un directorio, no un archivo."
            );
        }

        return fcb;
    }

    private void validarReferencia(FCB fcb) throws IOException {

        boolean sinBloques =
                fcb.getCantidadBloques() == 0
                && fcb.getBloqueInicial() == FCB.SIN_BLOQUE;

        if (sinBloques) {

            if (fcb.getTamanoBytes() != 0) {
                throw new IOException(
                        "Archivo inconsistente: tiene contenido "
                        + "pero no bloques."
                );
            }

            return;
        }

        if (fcb.getCantidadBloques() <= 0
                || fcb.getBloqueInicial() < 0
                || fcb.getTamanoBytes() <= 0) {

            throw new IOException(
                    "Referencias de contenido inconsistentes."
            );
        }

        long capacidad;

        switch (estrategia) {

            case "CONTIGUA":
                capacidad = fcb.getCantidadBloques()
                        * (long) bloques.getTamanoBloque();
                break;

            case "ENLAZADA":
                capacidad = fcb.getCantidadBloques()
                        * (long) enlazado.getCapacidadDatosPorBloque();
                break;

            case "INDEXADA":
                if (fcb.getCantidadBloques() < 2) {
                    throw new IOException(
                            "El archivo indexado necesita "
                            + "un bloque indice y uno de datos."
                    );
                }

                capacidad = (fcb.getCantidadBloques() - 1)
                        * (long) bloques.getTamanoBloque();
                break;

            default:
                throw new IOException(
                        "Estrategia no reconocida."
                );
        }

        if (fcb.getTamanoBytes() > capacidad) {
            throw new IOException(
                    "El tamano del archivo supera "
                    + "la capacidad de sus bloques."
            );
        }
    }

    private int capacidadUtil() throws IOException {

        switch (estrategia) {

            case "CONTIGUA":
            case "INDEXADA":
                return bloques.getTamanoBloque();

            case "ENLAZADA":
                return enlazado.getCapacidadDatosPorBloque();

            default:
                throw new IOException(
                        "Estrategia desconocida."
                );
        }
    }

    private static int calcularBloques(
            int bytes,
            int capacidad) {

        return (int) (
                ((long) bytes + capacidad - 1) / capacidad
        );
    }

    private long reservar(int cantidad) throws IOException {

        long inicio;

        switch (estrategia) {

            case "CONTIGUA":
                inicio = contiguo.reservar(cantidad);
                break;

            case "ENLAZADA":
                return enlazado.reservar(cantidad);

            case "INDEXADA":
                return indexado.reservar(cantidad);

            default:
                throw new IOException(
                        "Estrategia desconocida."
                );
        }

        if (inicio < 0) {
            throw new IOException(
                    "No existe espacio contiguo suficiente."
            );
        }

        return inicio;
    }

    private void escribirEnBloques(
            long inicio,
            long cantidadBloques,
            byte[] contenido) throws IOException {

        switch (estrategia) {

            case "CONTIGUA":

                int tamano = bloques.getTamanoBloque();
                int desplazamiento = 0;

                for (long i = 0; i < cantidadBloques; i++) {

                    int cantidad = Math.min(
                            tamano,
                            contenido.length - desplazamiento
                    );

                    byte[] fragmento = Arrays.copyOfRange(
                            contenido,
                            desplazamiento,
                            desplazamiento + cantidad
                    );

                    bloques.escribirBloque(
                            inicio + i,
                            fragmento
                    );

                    desplazamiento += cantidad;
                }

                break;

            case "ENLAZADA":
                enlazado.escribir(inicio, contenido);
                break;

            case "INDEXADA":
                indexado.escribir(inicio, contenido);
                break;

            default:
                throw new IOException(
                        "Estrategia desconocida."
                );
        }
    }

    private byte[] leerContiguo(
            long inicio,
            long cantidadBloques,
            int tamano) throws IOException {

        byte[] resultado = new byte[tamano];
        int desplazamiento = 0;

        for (long i = 0; i < cantidadBloques
                && desplazamiento < tamano; i++) {

            byte[] bloque = bloques.leerBloque(inicio + i);

            int cantidad = Math.min(
                    bloque.length,
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

    private void liberarReserva(
            long inicio,
            long cantidad) throws IOException {

        if (cantidad == 0) {
            return;
        }

        if (cantidad > Integer.MAX_VALUE) {
            throw new IOException(
                    "Cantidad de bloques fuera de rango."
            );
        }

        switch (estrategia) {

            case "CONTIGUA":
                contiguo.liberar(inicio, (int) cantidad);
                break;

            case "ENLAZADA":
                enlazado.liberar(inicio);
                break;

            case "INDEXADA":
                indexado.liberar(inicio);
                break;

            default:
                throw new IOException(
                        "Estrategia desconocida."
                );
        }
    }
}