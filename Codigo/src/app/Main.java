package app;

import config.ConfiguracionDisco;
import config.ConfiguracionDisco.EstrategiaAsignacion;
import config.ValidadorConfiguracion;
import modelo.disco.Superbloque;
import modelo.disco.DistribucionDisco;
import modelo.disco.RegistroMBR;
import almacenamiento.serializacion.SerializadorMBR;
import almacenamiento.serializacion.SerializadorSuperbloque;

/**
 * Punto de entrada del Sistema de Archivos Virtual.
 *
 * Por ahora ejecuta pruebas de configuracion y del superbloque.
 * Posteriormente se encargara de inicializar la aplicacion.
 */
public class Main {

    public static void main(String[] args) {

        System.out.println("======================================");
        System.out.println("       SISTEMA DE ARCHIVOS VIRTUAL    ");
        System.out.println("======================================");

        // Configuracion temporal para comprobar el modelo.
        // Posteriormente estos valores seran solicitados al usuario.
        ConfiguracionDisco configuracion = new ConfiguracionDisco(
                "miDiscoDuro.fs",
                "miFS",
                10L * 1024 * 1024,
                512,
                EstrategiaAsignacion.INDEXADA
        );

        // Validar la configuracion antes de continuar.
        try {
            ValidadorConfiguracion.validar(configuracion);
            System.out.println("Configuracion valida.");
        } catch (IllegalArgumentException e) {
            System.out.println("ERROR: " + e.getMessage());
            return;
        }

        // Mostrar la configuracion del disco.
        System.out.println();
        System.out.println("Configuracion del disco:");
        System.out.println("------------------------------");

        System.out.println("Nombre del disco: "
                + configuracion.getNombreDisco());

        System.out.println("File System: "
                + configuracion.getNombreFileSystem());

        System.out.println("Tamano (bytes): "
                + configuracion.getTamanoDiscoBytes());

        System.out.println("Tamano de bloque (bytes): "
                + configuracion.getTamanoBloqueBytes());

        System.out.println("Estrategia: "
                + configuracion.getEstrategiaAsignacion());

        System.out.println("Cantidad de bloques teoricos: "
                + configuracion.getCantidadBloques());


        // ==========================================
        // DISTRIBUCION FISICA DEL DISCO
        // ==========================================

        DistribucionDisco distribucion;

        try {
            distribucion = new DistribucionDisco(configuracion);
        } catch (IllegalArgumentException e) {
            System.out.println("ERROR: " + e.getMessage());
            return;
        }

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

        // ==========================================
        // SUPERBLOQUE CON VALORES CALCULADOS
        // ==========================================

        Superbloque superbloque = new Superbloque(
                configuracion,
                distribucion.getInicioBitmap(),
                distribucion.getInicioMetadatos(),
                distribucion.getInicioDatos(),
                distribucion.getCantidadBloquesDatos()
        );

        System.out.println();
        System.out.println("Informacion del superbloque:");
        System.out.println("------------------------------");

        System.out.println("Magic Number: "
                + Integer.toHexString(superbloque.getMagicNumber()));

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


        // ==========================================
        // PRUEBA DEL MBR SIMULADO
        // ==========================================

        long inicioParticion =
                distribucion.getInicioSuperbloque();

        long tamanoParticion =
                configuracion.getTamanoDiscoBytes()
                - inicioParticion;

        // Identificador temporal para la prueba.
        // En format se generara automaticamente.
        long identificadorDisco = 1001L;

        RegistroMBR mbr = new RegistroMBR(
                identificadorDisco,
                configuracion.getTamanoDiscoBytes(),
                inicioParticion,
                tamanoParticion
        );

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
        
        // ==========================================
        // PRUEBA DE SERIALIZACION BINARIA
        // ==========================================

        System.out.println();
        System.out.println("Prueba de serializacion binaria:");
        System.out.println("------------------------------");

        try {

            // 1. Serializar el MBR.
            byte[] bytesMBR = SerializadorMBR.serializar(mbr);

            // 2. Recuperar el MBR desde sus bytes.
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

            // 3. Serializar el superbloque.
            byte[] bytesSuperbloque =
                    SerializadorSuperbloque.serializar(superbloque);

            // 4. Recuperar el superbloque.
            Superbloque superbloqueRecuperado =
                    SerializadorSuperbloque.deserializar(
                            bytesSuperbloque);

            boolean superbloqueCorrecto =
                    superbloqueRecuperado.getMagicNumber()
                    == superbloque.getMagicNumber()
                    && superbloqueRecuperado.getNombreFileSystem()
                            .equals(superbloque.getNombreFileSystem())
                    && superbloqueRecuperado.getTamanoDiscoBytes()
                    == superbloque.getTamanoDiscoBytes()
                    && superbloqueRecuperado.getTamanoBloqueBytes()
                    == superbloque.getTamanoBloqueBytes()
                    && superbloqueRecuperado.getEstrategiaAsignacion()
                    == superbloque.getEstrategiaAsignacion()
                    && superbloqueRecuperado.getInicioBitmap()
                    == superbloque.getInicioBitmap()
                    && superbloqueRecuperado.getInicioMetadatos()
                    == superbloque.getInicioMetadatos()
                    && superbloqueRecuperado.getInicioDatos()
                    == superbloque.getInicioDatos()
                    && superbloqueRecuperado.getCantidadBloquesDatos()
                    == superbloque.getCantidadBloquesDatos();

            System.out.println("Superbloque serializado: "
                    + bytesSuperbloque.length + " bytes");

            System.out.println("Superbloque recuperado: "
                    + (superbloqueCorrecto
                            ? "CORRECTO" : "ERROR"));

            if (!mbrCorrecto || !superbloqueCorrecto) {
                System.out.println(
                        "ERROR: Los datos recuperados no coinciden.");
                return;
            }

        } catch (IllegalArgumentException e) {
            System.out.println("ERROR: " + e.getMessage());
            return;
        }
        
        System.out.println();
        System.out.println("Pruebas iniciales finalizadas.");
    }
}
