
package app;


import almacenamiento.DiscoVirtual;
import almacenamiento.GestorEspacioLibre;
import almacenamiento.GestorMetadatos;
import almacenamiento.GestorFCB;
import almacenamiento.FormateadorDisco;
import almacenamiento.MontadorDisco;
import almacenamiento.serializacion.SerializadorMBR;
import almacenamiento.serializacion.SerializadorSuperbloque;
import almacenamiento.serializacion.SerializadorUsuario;
import almacenamiento.serializacion.SerializadorGrupo;
import almacenamiento.serializacion.SerializadorFCB;

import config.ConfiguracionDisco;
import config.ConfiguracionDisco.EstrategiaAsignacion;
import config.ValidadorConfiguracion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import modelo.disco.DistribucionDisco;
import modelo.disco.DistribucionMetadatos;
import modelo.disco.RegistroMBR;
import modelo.disco.Superbloque;

import modelo.seguridad.Grupo;
import modelo.seguridad.SeguridadContrasenas;
import modelo.seguridad.Usuario;
import modelo.archivos.FCB;

/**
 * Punto de entrada del Sistema de Archivos Virtual.
 *
 * Ejecuta pruebas temporales de los componentes desarrollados.
 *
 * Posteriormente, Main solamente inicializara la aplicacion.
 */
public class Main {

    /**
     * Punto de entrada de NetBeans.
     * --test conserva todas las pruebas de regresion de la Fase 1.
     * La ejecucion normal se delega al lanzador.
     */
    public static void main(String[] args) {
        if (args != null && args.length == 1 && "--test".equals(args[0])) {
            ejecutarPruebas();
            return;
        }
        launcher.myFileSystem.iniciar(args);
    }

    /** Pruebas de regresion de la Fase 1. */
    private static void ejecutarPruebas() {

        System.out.println("======================================");
        System.out.println("       SISTEMA DE ARCHIVOS VIRTUAL    ");
        System.out.println("======================================");

        // Configuracion temporal para pruebas.
        ConfiguracionDisco configuracion = new ConfiguracionDisco(
                "miDiscoDuro.fs",
                "miFS",
                10L * 1024 * 1024,
                512,
                EstrategiaAsignacion.INDEXADA
        );

        try {

            ValidadorConfiguracion.validar(configuracion);
            System.out.println("Configuracion valida.");

            probarConfiguracion(configuracion);

            DistribucionDisco distribucion =
                    new DistribucionDisco(configuracion);

            probarDistribucion(distribucion);

            probarDistribucionMetadatos(distribucion);

            Superbloque superbloque = new Superbloque(
                    configuracion,
                    distribucion.getInicioBitmap(),
                    distribucion.getInicioMetadatos(),
                    distribucion.getInicioDatos(),
                    distribucion.getCantidadBloquesDatos()
            );

            probarSuperbloque(superbloque);

            long inicioParticion =
                    distribucion.getInicioSuperbloque();

            RegistroMBR mbr = new RegistroMBR(
                    1001L,
                    configuracion.getTamanoDiscoBytes(),
                    inicioParticion,
                    configuracion.getTamanoDiscoBytes()
                            - inicioParticion
            );

            probarMBR(mbr);

            probarSerializacion(mbr, superbloque);

            Path rutaDisco =
                    Path.of(configuracion.getNombreDisco());

            probarDiscoVirtual(
                    configuracion,
                    distribucion,
                    rutaDisco
            );

            probarBitmap(rutaDisco);

            Usuario rootPrueba = probarUsuario();

            probarGrupo(rootPrueba);

            probarSeguridadContrasenas();

            probarSerializacionUsuario();

            probarSerializacionGrupo();

            probarPersistenciaMetadatos();

            probarSerializacionFCB();

            probarPersistenciaFCB();

            probarFormateo();

            probarMontaje();

            System.out.println();
            System.out.println("Pruebas iniciales finalizadas.");

        } catch (IOException | RuntimeException e) {

            System.out.println();
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==========================================
    // PRUEBA DE CONFIGURACION
    // ==========================================

    private static void probarConfiguracion(
            ConfiguracionDisco config) {

        System.out.println();
        System.out.println("Configuracion del disco:");
        System.out.println("------------------------------");

        System.out.println("Nombre del disco: "
                + config.getNombreDisco());

        System.out.println("File System: "
                + config.getNombreFileSystem());

        System.out.println("Tamano (bytes): "
                + config.getTamanoDiscoBytes());

        System.out.println("Tamano de bloque (bytes): "
                + config.getTamanoBloqueBytes());

        System.out.println("Estrategia: "
                + config.getEstrategiaAsignacion());

        System.out.println("Cantidad de bloques teoricos: "
                + config.getCantidadBloques());
    }

    // ==========================================
    // PRUEBA DE DISTRIBUCION FISICA
    // ==========================================

    private static void probarDistribucion(
            DistribucionDisco distribucion) {

        System.out.println();
        System.out.println("Distribucion fisica del disco:");
        System.out.println("------------------------------");

        System.out.println("Inicio MBR: "
                + distribucion.getInicioMBR());

        System.out.println("Inicio Superbloque: "
                + distribucion.getInicioSuperbloque());

        System.out.println("Inicio Bitmap: "
                + distribucion.getInicioBitmap());

        System.out.println("Tamano Bitmap: "
                + distribucion.getTamanoBitmap());

        System.out.println("Inicio Metadatos: "
                + distribucion.getInicioMetadatos());

        System.out.println("Tamano Metadatos: "
                + distribucion.getTamanoMetadatos());

        System.out.println("Inicio Datos: "
                + distribucion.getInicioDatos());

        System.out.println("Cantidad de bloques de datos: "
                + distribucion.getCantidadBloquesDatos());

        System.out.println("Capacidad de datos (bytes): "
                + distribucion.getCapacidadDatosBytes());

        System.out.println("Espacio sobrante (bytes): "
                + distribucion.getEspacioSobranteBytes());
    }

    // ==========================================
    // PRUEBA DEL SUPERBLOQUE
    // ==========================================

    private static void probarSuperbloque(
            Superbloque superbloque) {

        System.out.println();
        System.out.println("Informacion del superbloque:");
        System.out.println("------------------------------");

        System.out.println("Magic Number: "
                + Integer.toHexString(
                        superbloque.getMagicNumber()));

        System.out.println("Version: "
                + superbloque.getVersion());

        System.out.println("Estrategia: "
                + superbloque.getEstrategiaAsignacion());

        System.out.println("Inicio Bitmap: "
                + superbloque.getInicioBitmap());

        System.out.println("Inicio Metadatos: "
                + superbloque.getInicioMetadatos());

        System.out.println("Inicio Datos: "
                + superbloque.getInicioDatos());

        System.out.println("Bloques de datos: "
                + superbloque.getCantidadBloquesDatos());
    }

    // ==========================================
    // PRUEBA DEL MBR
    // ==========================================

    private static void probarMBR(RegistroMBR mbr) {

        System.out.println();
        System.out.println("Informacion del MBR:");
        System.out.println("------------------------------");

        System.out.println("Magic Number: "
                + Integer.toHexString(mbr.getMagicNumber()));

        System.out.println("Version: "
                + mbr.getVersion());

        System.out.println("Identificador del disco: "
                + mbr.getIdentificadorDisco());

        System.out.println("Tamano del disco (bytes): "
                + mbr.getTamanoDiscoBytes());

        System.out.println("Inicio de particion: "
                + mbr.getInicioParticion());

        System.out.println("Tamano de particion: "
                + mbr.getTamanoParticion());

        System.out.println("Tipo de particion: 0x"
                + Integer.toHexString(mbr.getTipoParticion()));

        System.out.println("Firma MBR: 0x"
                + Integer.toHexString(mbr.getFirmaMBR()));
    }

    // ==========================================
    // PRUEBA DE SERIALIZACION BINARIA
    // ==========================================

    private static void probarSerializacion(
            RegistroMBR mbr,
            Superbloque superbloque) {

        System.out.println();
        System.out.println("Prueba de serializacion binaria:");
        System.out.println("------------------------------");

        byte[] bytesMBR =
                SerializadorMBR.serializar(mbr);

        RegistroMBR mbrRecuperado =
                SerializadorMBR.deserializar(bytesMBR);

        boolean mbrCorrecto =
                mbrRecuperado.getIdentificadorDisco()
                == mbr.getIdentificadorDisco()
                && mbrRecuperado.getTamanoDiscoBytes()
                == mbr.getTamanoDiscoBytes()
                && mbrRecuperado.getInicioParticion()
                == mbr.getInicioParticion()
                && mbrRecuperado.getTamanoParticion()
                == mbr.getTamanoParticion();

        System.out.println("MBR serializado: "
                + bytesMBR.length + " bytes");

        System.out.println("MBR recuperado: "
                + (mbrCorrecto ? "CORRECTO" : "ERROR"));

        byte[] bytesSuperbloque =
                SerializadorSuperbloque.serializar(superbloque);

        Superbloque recuperado =
                SerializadorSuperbloque.deserializar(
                        bytesSuperbloque
                );

        boolean superbloqueCorrecto =
                recuperado.getMagicNumber()
                == superbloque.getMagicNumber()
                && recuperado.getNombreFileSystem()
                        .equals(superbloque.getNombreFileSystem())
                && recuperado.getTamanoDiscoBytes()
                == superbloque.getTamanoDiscoBytes()
                && recuperado.getTamanoBloqueBytes()
                == superbloque.getTamanoBloqueBytes()
                && recuperado.getEstrategiaAsignacion()
                == superbloque.getEstrategiaAsignacion()
                && recuperado.getInicioBitmap()
                == superbloque.getInicioBitmap()
                && recuperado.getInicioMetadatos()
                == superbloque.getInicioMetadatos()
                && recuperado.getInicioDatos()
                == superbloque.getInicioDatos()
                && recuperado.getCantidadBloquesDatos()
                == superbloque.getCantidadBloquesDatos();

        System.out.println("Superbloque serializado: "
                + bytesSuperbloque.length + " bytes");

        System.out.println("Superbloque recuperado: "
                + (superbloqueCorrecto
                        ? "CORRECTO" : "ERROR"));

        if (!mbrCorrecto || !superbloqueCorrecto) {
            throw new IllegalStateException(
                    "Fallo la serializacion binaria.");
        }
    }

    // ==========================================
    // PRUEBA DEL DISCO VIRTUAL FISICO
    // ==========================================

    private static void probarDiscoVirtual(
            ConfiguracionDisco configuracion,
            DistribucionDisco distribucion,
            Path rutaDisco) throws IOException {

        System.out.println();
        System.out.println("Prueba del disco virtual:");
        System.out.println("------------------------------");

        if (!Files.exists(rutaDisco)) {

            try (DiscoVirtual disco =
                    DiscoVirtual.crear(configuracion)) {

                System.out.println(
                        "Disco creado correctamente.");

                System.out.println("Ruta: "
                        + disco.getRuta());

                System.out.println("ID del disco creado: "
                        + disco.getMbr().getIdentificadorDisco());
            }

        } else {

            System.out.println(
                    "El disco ya existe. Se abrira.");
        }

        try (DiscoVirtual disco =
                DiscoVirtual.abrir(rutaDisco)) {

            System.out.println(
                    "Disco abierto correctamente.");

            long tamanoFisico = Files.size(rutaDisco);

            System.out.println("Tamano fisico: "
                    + tamanoFisico + " bytes");

            System.out.println("ID del disco: "
                    + disco.getMbr().getIdentificadorDisco());

            System.out.println("File System: "
                    + disco.getSuperbloque()
                            .getNombreFileSystem());

            System.out.println("Estrategia recuperada: "
                    + disco.getSuperbloque()
                            .getEstrategiaAsignacion());

            System.out.println("Bloques de datos: "
                    + disco.getSuperbloque()
                            .getCantidadBloquesDatos());

            boolean tamanoCorrecto =
                    tamanoFisico
                    == configuracion.getTamanoDiscoBytes();

            boolean estructuraCorrecta =
                    disco.getSuperbloque().getInicioBitmap()
                    == distribucion.getInicioBitmap()
                    && disco.getSuperbloque().getInicioMetadatos()
                    == distribucion.getInicioMetadatos()
                    && disco.getSuperbloque().getInicioDatos()
                    == distribucion.getInicioDatos()
                    && disco.getSuperbloque()
                            .getCantidadBloquesDatos()
                    == distribucion.getCantidadBloquesDatos();

            if (!tamanoCorrecto || !estructuraCorrecta) {
                throw new IOException(
                        "Los datos recuperados no coinciden.");
            }

            System.out.println(
                    "Verificacion del disco: CORRECTO");
        }
    }

    // ==========================================
    // PRUEBA DEL BITMAP PERSISTENTE
    // ==========================================

    private static void probarBitmap(
            Path rutaDisco) throws IOException {

        System.out.println();
        System.out.println("Prueba del bitmap:");
        System.out.println("------------------------------");

        long bloqueReservado;

        try (DiscoVirtual disco =
                DiscoVirtual.abrir(rutaDisco)) {

            GestorEspacioLibre gestor =
                    new GestorEspacioLibre(disco);

            System.out.println("Total de bloques: "
                    + gestor.getCantidadBloques());

            System.out.println("Bloques ocupados antes: "
                    + gestor.contarBloquesOcupados());

            System.out.println("Bloques libres antes: "
                    + gestor.contarBloquesLibres());

            bloqueReservado =
                    gestor.reservarPrimerBloqueLibre();

            if (bloqueReservado == -1) {
                throw new IOException(
                        "No hay bloques disponibles.");
            }

            System.out.println("Bloque reservado: "
                    + bloqueReservado);

            System.out.println("Bloque ocupado: "
                    + gestor.estaOcupado(bloqueReservado));
        }

        try (DiscoVirtual disco =
                DiscoVirtual.abrir(rutaDisco)) {

            GestorEspacioLibre gestor =
                    new GestorEspacioLibre(disco);

            boolean persistente =
                    gestor.estaOcupado(bloqueReservado);

            System.out.println("Persistencia del bloque: "
                    + (persistente ? "CORRECTO" : "ERROR"));

            if (!persistente) {
                throw new IOException(
                        "El bitmap no conservo su estado.");
            }

            gestor.liberarBloque(bloqueReservado);

            System.out.println("Bloque liberado: "
                    + bloqueReservado);

            System.out.println("Bloques ocupados despues: "
                    + gestor.contarBloquesOcupados());

            System.out.println("Bloques libres despues: "
                    + gestor.contarBloquesLibres());
        }
    }

    // ==========================================
    // PRUEBA DEL MODELO DE USUARIO
    // ==========================================

    private static Usuario probarUsuario() {

        System.out.println();
        System.out.println("Prueba del modelo de usuario:");
        System.out.println("------------------------------");

        // Datos ficticios, no credenciales reales.
        byte[] hashPrueba = new byte[32];
        byte[] salPrueba = new byte[16];

        Usuario rootPrueba = new Usuario(
                0,
                "root",
                "Administrador del Sistema",
                0,
                hashPrueba,
                salPrueba,
                "/user/root",
                true
        );

        System.out.println("ID: "
                + rootPrueba.getId());

        System.out.println("Usuario: "
                + rootPrueba.getNombreUsuario());

        System.out.println("Nombre completo: "
                + rootPrueba.getNombreCompleto());

        System.out.println("Grupo principal: "
                + rootPrueba.getIdGrupoPrincipal());

        System.out.println("HOME: "
                + rootPrueba.getDirectorioHome());

        System.out.println("Administrador: "
                + rootPrueba.esAdministrador());

        System.out.println("Modelo de usuario: CORRECTO");

        return rootPrueba;
    }

    // ==========================================
    // PRUEBA DEL MODELO DE GRUPO
    // ==========================================

    private static void probarGrupo(Usuario rootPrueba) {

        System.out.println();
        System.out.println("Prueba del modelo de grupo:");
        System.out.println("------------------------------");

        Grupo grupoRoot = new Grupo(0, "root");

        System.out.println("ID del grupo: "
                + grupoRoot.getId());

        System.out.println("Nombre del grupo: "
                + grupoRoot.getNombre());

        boolean asociacionCorrecta =
                rootPrueba.getIdGrupoPrincipal()
                == grupoRoot.getId();

        System.out.println(
                "Usuario root pertenece al grupo root: "
                + asociacionCorrecta);

        if (!asociacionCorrecta) {
            throw new IllegalStateException(
                    "El usuario root no pertenece al grupo root.");
        }

        System.out.println("Modelo de grupo: CORRECTO");
    }

    // ==========================================
    // PRUEBA DE SEGURIDAD DE CONTRASENAS
    // ==========================================

    private static void probarSeguridadContrasenas() {

        System.out.println();
        System.out.println("Prueba de seguridad de contrasenas:");
        System.out.println("------------------------------");

        char[] contrasenaCorrecta =
                "ClavePrueba123!".toCharArray();

        char[] contrasenaIncorrecta =
                "ClaveEquivocada".toCharArray();

        byte[] sal = null;
        byte[] hash = null;

        try {

            sal = SeguridadContrasenas.generarSal();

            hash = SeguridadContrasenas.generarHash(
                    contrasenaCorrecta,
                    sal
            );

            boolean accesoCorrecto =
                    SeguridadContrasenas.verificar(
                            contrasenaCorrecta,
                            sal,
                            hash
                    );

            boolean accesoIncorrecto =
                    SeguridadContrasenas.verificar(
                            contrasenaIncorrecta,
                            sal,
                            hash
                    );

            System.out.println("Tamano de sal: "
                    + sal.length + " bytes");

            System.out.println("Tamano de hash: "
                    + hash.length + " bytes");

            System.out.println("Contrasena correcta: "
                    + (accesoCorrecto
                            ? "ACEPTADA" : "RECHAZADA"));

            System.out.println("Contrasena incorrecta: "
                    + (accesoIncorrecto
                            ? "ACEPTADA" : "RECHAZADA"));

            if (!accesoCorrecto || accesoIncorrecto) {

                throw new IllegalStateException(
                        "Fallo la verificacion de contrasenas.");
            }

            System.out.println(
                    "Seguridad de contrasenas: CORRECTO");

        } finally {

            Arrays.fill(contrasenaCorrecta, '\0');
            Arrays.fill(contrasenaIncorrecta, '\0');

            if (hash != null) {
                Arrays.fill(hash, (byte) 0);
            }

            if (sal != null) {
                Arrays.fill(sal, (byte) 0);
            }
        }
    }

    // ==========================================
    // PRUEBA DE SERIALIZACION DE USUARIOS
    // ==========================================

    private static void probarSerializacionUsuario() {

        System.out.println();
        System.out.println("Prueba de serializacion de usuario:");
        System.out.println("------------------------------");

        char[] contrasena =
                "ClavePrueba123!".toCharArray();

        byte[] sal = null;
        byte[] hash = null;

        try {

            sal = SeguridadContrasenas.generarSal();

            hash = SeguridadContrasenas.generarHash(
                    contrasena, sal);

            Usuario usuarioOriginal = new Usuario(
                    0,
                    "root",
                    "Administrador del Sistema",
                    0,
                    hash,
                    sal,
                    "/user/root",
                    true
            );

            byte[] datos =
                    SerializadorUsuario.serializar(
                            usuarioOriginal);

            Usuario recuperado =
                    SerializadorUsuario.deserializar(datos);

            boolean datosCorrectos =
                    recuperado.getId()
                    == usuarioOriginal.getId()
                    && recuperado.getNombreUsuario()
                            .equals(usuarioOriginal.getNombreUsuario())
                    && recuperado.getNombreCompleto()
                            .equals(usuarioOriginal.getNombreCompleto())
                    && recuperado.getIdGrupoPrincipal()
                    == usuarioOriginal.getIdGrupoPrincipal()
                    && recuperado.getDirectorioHome()
                            .equals(usuarioOriginal.getDirectorioHome())
                    && recuperado.esAdministrador()
                    == usuarioOriginal.esAdministrador()
                    && Arrays.equals(
                            recuperado.getHashContrasena(),
                            usuarioOriginal.getHashContrasena())
                    && Arrays.equals(
                            recuperado.getSalContrasena(),
                            usuarioOriginal.getSalContrasena());

            boolean contrasenaValida =
                    SeguridadContrasenas.verificar(
                            contrasena,
                            recuperado.getSalContrasena(),
                            recuperado.getHashContrasena()
                    );

            System.out.println("Tamano del registro: "
                    + datos.length + " bytes");

            System.out.println("Datos recuperados: "
                    + (datosCorrectos ? "CORRECTO" : "ERROR"));

            System.out.println("Contrasena recuperada: "
                    + (contrasenaValida ? "VALIDA" : "INVALIDA"));

            if (!datosCorrectos || !contrasenaValida) {
                throw new IllegalStateException(
                        "Fallo la serializacion del usuario.");
            }

            System.out.println(
                    "Serializacion de usuario: CORRECTO");

        } finally {

            Arrays.fill(contrasena, '\0');

            if (sal != null) {
                Arrays.fill(sal, (byte) 0);
            }

            if (hash != null) {
                Arrays.fill(hash, (byte) 0);
            }
        }
    }

    // ==========================================
    // PRUEBA DE SERIALIZACION DE GRUPOS
    // ==========================================

    private static void probarSerializacionGrupo() {

        System.out.println();
        System.out.println("Prueba de serializacion de grupo:");
        System.out.println("------------------------------");

        // Crear grupo de prueba.
        Grupo grupoOriginal = new Grupo(
                0,
                "root"
        );

        // Convertir el grupo a bytes.
        byte[] datos =
                SerializadorGrupo.serializar(
                        grupoOriginal
                );

        // Recuperar el grupo desde sus bytes.
        Grupo grupoRecuperado =
                SerializadorGrupo.deserializar(datos);

        // Verificar los atributos recuperados.
        boolean datosCorrectos =
                grupoRecuperado.getId()
                == grupoOriginal.getId()
                && grupoRecuperado.getNombre()
                        .equals(grupoOriginal.getNombre());

        System.out.println("Tamano del registro: "
                + datos.length + " bytes");

        System.out.println("ID recuperado: "
                + grupoRecuperado.getId());

        System.out.println("Nombre recuperado: "
                + grupoRecuperado.getNombre());

        System.out.println("Datos recuperados: "
                + (datosCorrectos ? "CORRECTO" : "ERROR"));

        if (!datosCorrectos) {
            throw new IllegalStateException(
                    "Fallo la serializacion del grupo.");
        }

        System.out.println(
                "Serializacion de grupo: CORRECTO");
    }

    // ==========================================
    // PRUEBA DE DISTRIBUCION DE METADATOS
    // ==========================================

    private static void probarDistribucionMetadatos(
            DistribucionDisco distribucion) {

        System.out.println();
        System.out.println("Prueba de distribucion de metadatos:");
        System.out.println("------------------------------");

        DistribucionMetadatos metadatos =
                new DistribucionMetadatos(distribucion);

        System.out.println("Inicio de metadatos: "
                + metadatos.getInicioMetadatos());

        System.out.println("Tamano total: "
                + metadatos.getTamanoMetadatos());

        System.out.println("Inicio de grupos: "
                + metadatos.getInicioGrupos());

        System.out.println("Capacidad de grupos: "
                + metadatos.getCantidadGrupos());

        System.out.println("Inicio de usuarios: "
                + metadatos.getInicioUsuarios());

        System.out.println("Capacidad de usuarios: "
                + metadatos.getCantidadUsuarios());

        System.out.println("Inicio de FCB: "
                + metadatos.getInicioFCB());

        System.out.println("Capacidad de FCB: "
                + metadatos.getCantidadFCB());

        System.out.println("Offset del grupo root: "
                + metadatos.obtenerOffsetGrupo(0));

        System.out.println("Offset del usuario root: "
                + metadatos.obtenerOffsetUsuario(0));

        System.out.println("Espacio sin asignar: "
                + metadatos.getEspacioSinAsignar());

        boolean estructuraCorrecta =
                metadatos.getInicioGrupos()
                == metadatos.getInicioMetadatos()
                        + DistribucionMetadatos.TAMANO_ENCABEZADO
                && metadatos.getInicioUsuarios()
                == metadatos.getInicioGrupos()
                        + metadatos.getTamanoTablaGrupos()
                && metadatos.getInicioFCB()
                == metadatos.getInicioUsuarios()
                        + metadatos.getTamanoTablaUsuarios()
                && metadatos.getInicioFCB()
                        + metadatos.getTamanoTablaFCB()
                <= metadatos.getFinMetadatos()
                && metadatos.getFinMetadatos()
                == distribucion.getInicioDatos();

        if (!estructuraCorrecta) {
            throw new IllegalStateException(
                    "Las regiones de metadatos se superponen "
                    + "o exceden sus limites.");
        }

        System.out.println(
                "Distribucion de metadatos: CORRECTO");
    }

    // ==========================================
    // PRUEBA DE PERSISTENCIA FISICA DE METADATOS
    // ==========================================

    private static void probarPersistenciaMetadatos() throws IOException {

        System.out.println();
        System.out.println("Prueba de persistencia de metadatos:");
        System.out.println("------------------------------");

        // Disco de prueba independiente: no altera miDiscoDuro.fs.
        String nombrePrueba = "pruebaMetadatos_"
                + System.nanoTime() + ".fs";

        ConfiguracionDisco configPrueba = new ConfiguracionDisco(
                nombrePrueba, "fsPrueba", 1024L * 1024,
                512, EstrategiaAsignacion.INDEXADA
        );
        Path rutaPrueba = Path.of(nombrePrueba);

        char[] contrasena = "ClavePrueba123!".toCharArray();
        byte[] sal = null;
        byte[] hash = null;

        try {
            ValidadorConfiguracion.validar(configPrueba);
            sal = SeguridadContrasenas.generarSal();
            hash = SeguridadContrasenas.generarHash(contrasena, sal);

            Grupo grupoOriginal = new Grupo(0, "root");
            Usuario usuarioOriginal = new Usuario(
                    0, "root", "Administrador del Sistema", 0,
                    hash, sal, "/user/root", true
            );

            // Primera apertura: escribir registros binarios.
            try (DiscoVirtual disco = DiscoVirtual.crear(configPrueba)) {
                GestorMetadatos gestor = new GestorMetadatos(disco);

                if (gestor.leerGrupo(0) != null
                        || gestor.leerUsuario(0) != null) {
                    throw new IllegalStateException(
                            "El disco nuevo contiene registros ocupados.");
                }

                gestor.guardarGrupo(0, grupoOriginal);
                gestor.guardarUsuario(0, usuarioOriginal);

                System.out.println("Offset del grupo root: "
                        + gestor.getDistribucion().obtenerOffsetGrupo(0));
                System.out.println("Offset del usuario root: "
                        + gestor.getDistribucion().obtenerOffsetUsuario(0));
                System.out.println("Registros escritos en el archivo: CORRECTO");
            }

            // Segunda apertura: recuperar registros desde el archivo .fs.
            try (DiscoVirtual disco = DiscoVirtual.abrir(rutaPrueba)) {
                GestorMetadatos gestor = new GestorMetadatos(disco);
                Grupo grupo = gestor.leerGrupo(0);
                Usuario usuario = gestor.leerUsuario(0);

                boolean grupoCorrecto = grupo != null
                        && grupo.getId() == 0
                        && "root".equals(grupo.getNombre());

                boolean usuarioCorrecto = usuario != null
                        && usuario.getId() == 0
                        && "root".equals(usuario.getNombreUsuario())
                        && usuario.getIdGrupoPrincipal() == 0
                        && "/user/root".equals(usuario.getDirectorioHome())
                        && usuario.esAdministrador()
                        && Arrays.equals(hash, usuario.getHashContrasena())
                        && Arrays.equals(sal, usuario.getSalContrasena());

                boolean claveCorrecta = usuarioCorrecto
                        && SeguridadContrasenas.verificar(
                                contrasena,
                                usuario.getSalContrasena(),
                                usuario.getHashContrasena()
                        );

                System.out.println("Grupo recuperado: "
                        + (grupoCorrecto ? "CORRECTO" : "ERROR"));
                System.out.println("Usuario recuperado: "
                        + (usuarioCorrecto ? "CORRECTO" : "ERROR"));
                System.out.println("Contrasena recuperada: "
                        + (claveCorrecta ? "VALIDA" : "INVALIDA"));

                if (!grupoCorrecto || !usuarioCorrecto || !claveCorrecta) {
                    throw new IllegalStateException(
                            "Fallo la recuperacion de metadatos del disco.");
                }

                // Intentar sobrescribir un registro debe fallar.
                boolean bloqueoCorrecto = false;
                try {
                    gestor.guardarGrupo(0, grupoOriginal);
                } catch (IllegalStateException esperado) {
                    bloqueoCorrecto = true;
                }

                if (!bloqueoCorrecto) {
                    throw new IllegalStateException(
                            "Se permitio sobrescribir un grupo existente.");
                }
                System.out.println("Proteccion contra sobrescritura: CORRECTO");
            }

            System.out.println("Persistencia fisica de metadatos: CORRECTO");

        } finally {
            Arrays.fill(contrasena, '\0');
            if (sal != null) {
                Arrays.fill(sal, (byte) 0);
            }
            if (hash != null) {
                Arrays.fill(hash, (byte) 0);
            }
            // Eliminar unicamente el disco temporal de esta prueba.
            Files.deleteIfExists(rutaPrueba);
        }
    }

    // ==========================================
    // PRUEBA DE SERIALIZACION DE FCB
    // ==========================================

    private static void probarSerializacionFCB() {

        System.out.println();
        System.out.println("Prueba de serializacion de FCB:");
        System.out.println("------------------------------");

        FCB raiz = FCB.crearRaiz(0, 0);
        FCB carpeta = FCB.crearDirectorio(1, 0, "user", 0, 0, 0755);
        FCB archivo = FCB.crearArchivo(2, 1, "nota.txt", 0, 0, 0644);

        verificarFCB(raiz);
        verificarFCB(carpeta);
        verificarFCB(archivo);

        System.out.println("Registro binario: "
                + SerializadorFCB.TAMANO + " bytes");
        System.out.println("Directorio raiz: CORRECTO");
        System.out.println("Directorio hijo: CORRECTO");
        System.out.println("Archivo: CORRECTO");

        // Una cabecera alterada debe ser rechazada.
        byte[] invalido = SerializadorFCB.serializar(raiz);
        invalido[0] = 0;
        boolean rechazoCorrecto = false;
        try {
            SerializadorFCB.deserializar(invalido);
        } catch (IllegalArgumentException esperado) {
            rechazoCorrecto = true;
        }
        if (!rechazoCorrecto) {
            throw new IllegalStateException(
                    "No se detecto un FCB con magic invalido.");
        }

        System.out.println("Validacion de encabezado: CORRECTO");
        System.out.println("Serializacion de FCB: CORRECTO");
    }

    private static void verificarFCB(FCB original) {

        byte[] bytes = SerializadorFCB.serializar(original);
        if (bytes.length != SerializadorFCB.TAMANO) {
            throw new IllegalStateException("Tamano de FCB incorrecto.");
        }
        FCB recuperado = SerializadorFCB.deserializar(bytes);
        boolean correcto = recuperado.getId() == original.getId()
                && recuperado.getIdPadre() == original.getIdPadre()
                && recuperado.getNombre().equals(original.getNombre())
                && recuperado.getTipo() == original.getTipo()
                && recuperado.getIdPropietario() == original.getIdPropietario()
                && recuperado.getIdGrupo() == original.getIdGrupo()
                && recuperado.getPermisos() == original.getPermisos()
                && recuperado.getTamanoBytes() == original.getTamanoBytes()
                && recuperado.getBloqueInicial() == original.getBloqueInicial()
                && recuperado.getCantidadBloques() == original.getCantidadBloques()
                && recuperado.getFechaCreacion() == original.getFechaCreacion()
                && recuperado.getFechaModificacion() == original.getFechaModificacion()
                && recuperado.getFechaAcceso() == original.getFechaAcceso()
                && recuperado.getCantidadEnlaces() == original.getCantidadEnlaces();

        if (!correcto) {
            throw new IllegalStateException(
                    "El FCB recuperado no coincide: " + original.getNombre());
        }
    }
    // ==========================================
    // PRUEBA DE PERSISTENCIA FISICA DE FCB
    // ==========================================

    private static void probarPersistenciaFCB() throws IOException {

        System.out.println();
        System.out.println("Prueba de persistencia de FCB:");
        System.out.println("------------------------------");

        // Nunca modifica el disco principal miDiscoDuro.fs.
        String nombrePrueba = "pruebaFCB_"
                + java.util.UUID.randomUUID().toString().replace("-", "")
                + ".fs";
        Path rutaPrueba = Path.of(nombrePrueba);
        ConfiguracionDisco configPrueba = new ConfiguracionDisco(
                nombrePrueba, "fsPrueba", 1024L * 1024,
                512, EstrategiaAsignacion.INDEXADA
        );

        FCB raiz = FCB.crearRaiz(0, 0);
        FCB user = FCB.crearDirectorio(1, 0, "user", 0, 0, 0755);
        FCB root = FCB.crearDirectorio(2, 1, "root", 0, 0, 0700);

        boolean discoCreado = false;
        try {
            ValidadorConfiguracion.validar(configPrueba);

            try (DiscoVirtual disco = DiscoVirtual.crear(configPrueba)) {
                discoCreado = true;
                GestorFCB gestor = new GestorFCB(disco);
                if (gestor.leerFCB(0) != null) {
                    throw new IllegalStateException(
                            "Un disco nuevo contiene un FCB ocupado.");
                }

                gestor.guardarFCB(0, raiz);
                gestor.guardarFCB(1, user);
                gestor.guardarFCB(2, root);

                System.out.println("Offset FCB raiz: "
                        + gestor.getDistribucion().obtenerOffsetFCB(0));
                System.out.println("Offset FCB /user: "
                        + gestor.getDistribucion().obtenerOffsetFCB(1));
                System.out.println("Offset FCB /user/root: "
                        + gestor.getDistribucion().obtenerOffsetFCB(2));
                System.out.println("FCB escritos en el archivo: CORRECTO");
            }

            // Abrir de nuevo el .fs y comparar TODOS los atributos.
            try (DiscoVirtual disco = DiscoVirtual.abrir(rutaPrueba)) {
                GestorFCB gestor = new GestorFCB(disco);
                compararFCBRecuperado(raiz, gestor.leerFCB(0));
                compararFCBRecuperado(user, gestor.leerFCB(1));
                compararFCBRecuperado(root, gestor.leerFCB(2));

                System.out.println("Raiz recuperada: CORRECTO");
                System.out.println("Directorio /user recuperado: CORRECTO");
                System.out.println("Directorio /user/root recuperado: CORRECTO");

                boolean rechazoSobrescritura = false;
                try {
                    gestor.guardarFCB(1, user);
                } catch (IllegalStateException esperado) {
                    rechazoSobrescritura = true;
                }
                if (!rechazoSobrescritura) {
                    throw new IllegalStateException(
                            "Se permitio sobrescribir un FCB ocupado.");
                }
                System.out.println("Proteccion contra sobrescritura: CORRECTO");

                boolean rechazoIndice = false;
                try {
                    gestor.leerFCB(gestor.getDistribucion().getCantidadFCB());
                } catch (IndexOutOfBoundsException esperado) {
                    rechazoIndice = true;
                }
                if (!rechazoIndice) {
                    throw new IllegalStateException(
                            "Se permitio acceder fuera de la tabla FCB.");
                }
                System.out.println("Validacion de limites: CORRECTO");
            }

            System.out.println("Persistencia fisica de FCB: CORRECTO");
        } finally {
            if (discoCreado) {
                Files.deleteIfExists(rutaPrueba);
            }
        }
    }

    private static void compararFCBRecuperado(
            FCB original, FCB recuperado) {
        if (recuperado == null) {
            throw new IllegalStateException(
                    "No se recupero el FCB: " + original.getNombre());
        }
        // Reutiliza la comparacion de serializacion sin tocar el disco.
        byte[] originalBytes = SerializadorFCB.serializar(original);
        byte[] recuperadoBytes = SerializadorFCB.serializar(recuperado);
        if (!Arrays.equals(originalBytes, recuperadoBytes)) {
            throw new IllegalStateException(
                    "El FCB recuperado difiere: " + original.getNombre());
        }
    }


    // ==========================================
    // PRUEBA DEL FORMATEADOR DE DISCO
    // ==========================================

    private static void probarFormateo() throws IOException {

        System.out.println();
        System.out.println("Prueba de formateo completo:");
        System.out.println("------------------------------");

        // Se utiliza un disco temporal, nunca miDiscoDuro.fs.
        String nombrePrueba = "pruebaFormato_"
                + java.util.UUID.randomUUID().toString().replace("-", "")
                + ".fs";
        ConfiguracionDisco configuracion = new ConfiguracionDisco(
                nombrePrueba, "fsFormateado", 1024L * 1024,
                512, EstrategiaAsignacion.INDEXADA
        );

        Path ruta = Path.of(nombrePrueba);
        char[] contrasena = "ClavePrueba123!".toCharArray();
        boolean creado = false;

        try {
            // Este metodo crea el volumen y todas las estructuras iniciales.
            Path resultado = FormateadorDisco.formatear(
                    configuracion, contrasena);
            creado = true;

            if (!resultado.equals(ruta) || !Files.exists(ruta)) {
                throw new IllegalStateException(
                        "No se creo el archivo .fs solicitado.");
            }
            System.out.println("Disco formateado: CORRECTO");

            // Cerrar y abrir es parte de la prueba real de persistencia.
            try (DiscoVirtual disco = DiscoVirtual.abrir(ruta)) {
                GestorMetadatos metadatos = new GestorMetadatos(disco);
                GestorFCB gestorFCB = new GestorFCB(disco);
                GestorEspacioLibre bitmap = new GestorEspacioLibre(disco);

                Grupo grupo = metadatos.leerGrupo(0);
                Usuario usuario = metadatos.leerUsuario(0);
                FCB raiz = gestorFCB.leerFCB(0);
                FCB user = gestorFCB.leerFCB(1);
                FCB root = gestorFCB.leerFCB(2);

                boolean grupoCorrecto = grupo != null
                        && grupo.getId() == 0
                        && "root".equals(grupo.getNombre());
                boolean usuarioCorrecto = usuario != null
                        && usuario.getId() == 0
                        && "root".equals(usuario.getNombreUsuario())
                        && usuario.getIdGrupoPrincipal() == 0
                        && "/user/root".equals(usuario.getDirectorioHome())
                        && usuario.esAdministrador();
                boolean claveCorrecta = usuarioCorrecto
                        && SeguridadContrasenas.verificar(
                                contrasena,
                                usuario.getSalContrasena(),
                                usuario.getHashContrasena());
                boolean directoriosCorrectos = raiz != null
                        && raiz.esDirectorio()
                        && raiz.getId() == 0
                        && raiz.getIdPadre() == -1
                        && "/".equals(raiz.getNombre())
                        && user != null
                        && user.esDirectorio()
                        && user.getId() == 1
                        && user.getIdPadre() == 0
                        && "user".equals(user.getNombre())
                        && root != null
                        && root.esDirectorio()
                        && root.getId() == 2
                        && root.getIdPadre() == 1
                        && "root".equals(root.getNombre())
                        && root.getPermisos() == 0700;
                boolean bitmapCorrecto =
                        bitmap.contarBloquesOcupados() == 0;

                if (!grupoCorrecto || !usuarioCorrecto || !claveCorrecta
                        || !directoriosCorrectos || !bitmapCorrecto) {
                    throw new IllegalStateException(
                            "El disco formateado no recupero sus estructuras.");
                }

                System.out.println("Grupo root persistente: CORRECTO");
                System.out.println("Usuario root persistente: CORRECTO");
                System.out.println("Contrasena root: VALIDA");
                System.out.println("Directorio /: CORRECTO");
                System.out.println("Directorio /user: CORRECTO");
                System.out.println("Directorio /user/root: CORRECTO");
                System.out.println("Bitmap inicial: CORRECTO");
            }

            // Verificar que la segunda llamada NO destruya el disco existente.
            byte[] mbrAntes;
            try (DiscoVirtual disco = DiscoVirtual.abrir(ruta)) {
                mbrAntes = disco.leer(0, 512);
            }
            boolean rechazoSobrescritura = false;
            try {
                FormateadorDisco.formatear(configuracion, contrasena);
            } catch (IOException esperado) {
                rechazoSobrescritura = true;
            }
            if (!rechazoSobrescritura) {
                throw new IllegalStateException(
                        "El formateador permitio sobrescribir el disco.");
            }
            try (DiscoVirtual disco = DiscoVirtual.abrir(ruta)) {
                if (!Arrays.equals(mbrAntes, disco.leer(0, 512))) {
                    throw new IllegalStateException(
                            "El segundo formateo altero el disco existente.");
                }
            }
            System.out.println("Proteccion del disco existente: CORRECTO");
            System.out.println("Formateo inicial persistente: CORRECTO");

        } finally {
            Arrays.fill(contrasena, '\0');
            if (creado) {
                Files.deleteIfExists(ruta);
            }
        }
    }

    // ==========================================
    // PRUEBA DEL MONTAJE INTEGRAL
    // ==========================================

    private static void probarMontaje() throws IOException {

        System.out.println();
        System.out.println("Prueba de montaje integral:");
        System.out.println("------------------------------");

        String nombre = "pruebaMontaje_"
                + java.util.UUID.randomUUID().toString().replace("-", "")
                + ".fs";
        Path ruta = Path.of(nombre);
        ConfiguracionDisco config = new ConfiguracionDisco(
                nombre, "fsMontaje", 1024L * 1024,
                512, EstrategiaAsignacion.INDEXADA);
        char[] clave = "ClavePrueba123!".toCharArray();
        boolean creado = false;

        try {
            FormateadorDisco.formatear(config, clave);
            creado = true;

            try (MontadorDisco montaje = MontadorDisco.montar(ruta)) {
                System.out.println("MBR y superbloque: CORRECTO");
                System.out.println("Grupo root: "
                        + montaje.getGrupoRoot().getNombre());
                System.out.println("Usuario root: "
                        + montaje.getRoot().getNombreUsuario());

                boolean claveCorrecta = SeguridadContrasenas.verificar(
                        clave,
                        montaje.getRoot().getSalContrasena(),
                        montaje.getRoot().getHashContrasena());
                if (!claveCorrecta) {
                    throw new IllegalStateException(
                            "No se pudo verificar la clave root montada.");
                }
                System.out.println("Contrasena root: VALIDA");
                System.out.println("Directorio raiz: "
                        + montaje.getDirectorioRaiz().getNombre());
                System.out.println("Directorio HOME: /user/"
                        + montaje.getDirectorioRoot().getNombre());
                System.out.println("Bitmap accesible: CORRECTO");
            }

            // Un volumen con MBR y superbloque pero sin format
            // no puede pasar la validacion de montaje.
            String nombreSinFormato = "pruebaSinFormato_"
                    + java.util.UUID.randomUUID().toString().replace("-", "")
                    + ".fs";
            Path rutaSinFormato = Path.of(nombreSinFormato);
            ConfiguracionDisco sinFormato = new ConfiguracionDisco(
                    nombreSinFormato, "fsVacio", 1024L * 1024,
                    512, EstrategiaAsignacion.INDEXADA);
            boolean creadoSinFormato = false;
            try {
                try (DiscoVirtual disco = DiscoVirtual.crear(sinFormato)) {
                    creadoSinFormato = true;
                }
                boolean rechazado = false;
                try (MontadorDisco ignorado = MontadorDisco.montar(rutaSinFormato)) {
                    // Debe fallar al faltar las estructuras iniciales.
                } catch (IOException esperado) {
                    rechazado = true;
                }
                if (!rechazado) {
                    throw new IllegalStateException(
                            "El montaje acepto un volumen sin formatear.");
                }
                System.out.println("Volumen sin formatear rechazado: CORRECTO");
            } finally {
                if (creadoSinFormato) {
                    Files.deleteIfExists(rutaSinFormato);
                }
            }

            System.out.println("Montaje integral: CORRECTO");

        } finally {
            Arrays.fill(clave, '\0');
            if (creado) {
                Files.deleteIfExists(ruta);
            }
        }
    }

}
