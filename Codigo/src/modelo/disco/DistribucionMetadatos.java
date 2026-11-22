package modelo.disco;

/**
 * Define la distribucion interna de la region de metadatos.
 *
 * Las tablas de grupos, usuarios y FCB ocupan posiciones
 * fijas dentro de cada disco, calculadas segun su tamano.
 *
 * Esta clase no escribe ni lee datos del disco.
 */
public final class DistribucionMetadatos {

    public static final int TAMANO_ENCABEZADO = 512;
    public static final int TAMANO_REGISTRO_GRUPO = 128;
    public static final int TAMANO_REGISTRO_USUARIO = 512;
    public static final int TAMANO_REGISTRO_FCB = 512;

    private final long inicioMetadatos;
    private final long tamanoMetadatos;
    private final long finMetadatos;

    private final long inicioEncabezado;

    private final long inicioGrupos;
    private final long cantidadGrupos;
    private final long tamanoTablaGrupos;

    private final long inicioUsuarios;
    private final long cantidadUsuarios;
    private final long tamanoTablaUsuarios;

    private final long inicioFCB;
    private final long cantidadFCB;
    private final long tamanoTablaFCB;

    /**
     * Calcula la distribucion usando el diseno fisico
     * de un disco que se esta creando.
     */
    public DistribucionMetadatos(
            DistribucionDisco distribucion) {

        this(
                obtenerInicio(distribucion),
                obtenerTamano(distribucion)
        );
    }

    /**
     * Recupera la distribucion a partir del superbloque
     * de un disco existente.
     */
    public DistribucionMetadatos(
            Superbloque superbloque) {

        this(
                obtenerInicio(superbloque),
                obtenerTamano(superbloque)
        );
    }

    /**
     * Calcula las regiones internas de metadatos.
     */
    public DistribucionMetadatos(
            long inicioMetadatos,
            long tamanoMetadatos) {

        if (inicioMetadatos < 0 || tamanoMetadatos <= 0) {
            throw new IllegalArgumentException(
                    "La region de metadatos es invalida.");
        }

        if (inicioMetadatos > Long.MAX_VALUE - tamanoMetadatos) {
            throw new IllegalArgumentException(
                    "La region de metadatos excede los limites.");
        }

        this.inicioMetadatos = inicioMetadatos;
        this.tamanoMetadatos = tamanoMetadatos;
        this.finMetadatos =
                inicioMetadatos + tamanoMetadatos;

        // Reservar un encabezado de 512 bytes.
        inicioEncabezado = inicioMetadatos;

        if (tamanoMetadatos <= TAMANO_ENCABEZADO) {
            throw new IllegalArgumentException(
                    "No hay espacio suficiente para metadatos.");
        }

        long espacioDisponible =
                tamanoMetadatos - TAMANO_ENCABEZADO;

        // =====================================
        // TABLA DE GRUPOS: 10 %
        // =====================================

        long presupuestoGrupos =
                espacioDisponible / 10;

        cantidadGrupos =
                presupuestoGrupos / TAMANO_REGISTRO_GRUPO;

        tamanoTablaGrupos =
                cantidadGrupos * TAMANO_REGISTRO_GRUPO;

        inicioGrupos =
                inicioMetadatos + TAMANO_ENCABEZADO;

        // =====================================
        // TABLA DE USUARIOS: 20 %
        // =====================================

        long presupuestoUsuarios =
                espacioDisponible / 5;

        cantidadUsuarios =
                presupuestoUsuarios / TAMANO_REGISTRO_USUARIO;

        tamanoTablaUsuarios =
                cantidadUsuarios * TAMANO_REGISTRO_USUARIO;

        inicioUsuarios =
                inicioGrupos + tamanoTablaGrupos;

        // =====================================
        // TABLA DE FCB: ESPACIO RESTANTE
        // =====================================

        inicioFCB =
                inicioUsuarios + tamanoTablaUsuarios;

        long espacioRestante =
                finMetadatos - inicioFCB;

        cantidadFCB =
                espacioRestante / TAMANO_REGISTRO_FCB;

        tamanoTablaFCB =
                cantidadFCB * TAMANO_REGISTRO_FCB;

        // Se requiere al menos un registro de cada tipo.
        if (cantidadGrupos < 1
                || cantidadUsuarios < 1
                || cantidadFCB < 1) {

            throw new IllegalArgumentException(
                    "El disco no tiene espacio suficiente "
                    + "para las tablas de metadatos.");
        }
    }

    private static long obtenerInicio(
            DistribucionDisco distribucion) {

        if (distribucion == null) {
            throw new IllegalArgumentException(
                    "La distribucion no puede ser nula.");
        }

        return distribucion.getInicioMetadatos();
    }

    private static long obtenerTamano(
            DistribucionDisco distribucion) {

        if (distribucion == null) {
            throw new IllegalArgumentException(
                    "La distribucion no puede ser nula.");
        }

        return distribucion.getTamanoMetadatos();
    }

    private static long obtenerInicio(
            Superbloque superbloque) {

        if (superbloque == null) {
            throw new IllegalArgumentException(
                    "El superbloque no puede ser nulo.");
        }

        return superbloque.getInicioMetadatos();
    }

    private static long obtenerTamano(
            Superbloque superbloque) {

        if (superbloque == null) {
            throw new IllegalArgumentException(
                    "El superbloque no puede ser nulo.");
        }

        return superbloque.getInicioDatos()
                - superbloque.getInicioMetadatos();
    }

    /**
     * Obtiene el offset fisico de un registro de grupo.
     */
    public long obtenerOffsetGrupo(long indice) {

        validarIndice(indice, cantidadGrupos);

        return inicioGrupos
                + indice * TAMANO_REGISTRO_GRUPO;
    }

    /**
     * Obtiene el offset fisico de un registro de usuario.
     */
    public long obtenerOffsetUsuario(long indice) {

        validarIndice(indice, cantidadUsuarios);

        return inicioUsuarios
                + indice * TAMANO_REGISTRO_USUARIO;
    }

    /**
     * Obtiene el offset fisico de un FCB.
     */
    public long obtenerOffsetFCB(long indice) {

        validarIndice(indice, cantidadFCB);

        return inicioFCB
                + indice * TAMANO_REGISTRO_FCB;
    }

    private static void validarIndice(
            long indice, long cantidad) {

        if (indice < 0 || indice >= cantidad) {
            throw new IndexOutOfBoundsException(
                    "Indice fuera de la tabla de metadatos.");
        }
    }

    public long getInicioMetadatos() {
        return inicioMetadatos;
    }

    public long getTamanoMetadatos() {
        return tamanoMetadatos;
    }

    public long getFinMetadatos() {
        return finMetadatos;
    }

    public long getInicioEncabezado() {
        return inicioEncabezado;
    }

    public long getInicioGrupos() {
        return inicioGrupos;
    }

    public long getCantidadGrupos() {
        return cantidadGrupos;
    }

    public long getTamanoTablaGrupos() {
        return tamanoTablaGrupos;
    }

    public long getInicioUsuarios() {
        return inicioUsuarios;
    }

    public long getCantidadUsuarios() {
        return cantidadUsuarios;
    }

    public long getTamanoTablaUsuarios() {
        return tamanoTablaUsuarios;
    }

    public long getInicioFCB() {
        return inicioFCB;
    }

    public long getCantidadFCB() {
        return cantidadFCB;
    }

    public long getTamanoTablaFCB() {
        return tamanoTablaFCB;
    }

    public long getEspacioSinAsignar() {

        return finMetadatos
                - (inicioFCB + tamanoTablaFCB);
    }
}