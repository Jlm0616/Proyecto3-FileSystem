package almacenamiento;

import almacenamiento.serializacion.SerializadorMBR;
import almacenamiento.serializacion.SerializadorSuperbloque;
import config.ConfiguracionDisco;
import config.ValidadorConfiguracion;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;
import modelo.disco.DistribucionDisco;
import modelo.disco.RegistroMBR;
import modelo.disco.Superbloque;

/**
 * Gestiona la creacion, apertura y cierre del disco virtual.
 *
 * Su responsabilidad es acceder al archivo fisico .fs
 * y almacenar o recuperar las estructuras principales.
 *
 * No administra archivos internos, directorios ni usuarios.
 */
public final class DiscoVirtual implements AutoCloseable {

    private final Path ruta;
    private final RandomAccessFile archivo;
    private final RegistroMBR mbr;
    private final Superbloque superbloque;

    private DiscoVirtual(
            Path ruta,
            RandomAccessFile archivo,
            RegistroMBR mbr,
            Superbloque superbloque) {

        this.ruta = ruta;
        this.archivo = archivo;
        this.mbr = mbr;
        this.superbloque = superbloque;
    }

    /**
     * Crea un nuevo disco virtual y escribe las
     * estructuras iniciales en formato binario.
     *
     * No sobrescribe discos existentes.
     */
    public static DiscoVirtual crear(
            ConfiguracionDisco configuracion) throws IOException {

        ValidadorConfiguracion.validar(configuracion);

        DistribucionDisco distribucion =
                new DistribucionDisco(configuracion);

        Path ruta = Path.of(
                configuracion.getNombreDisco()
        ).toAbsolutePath();

        // Generar un identificador positivo para el disco.
        long identificadorDisco =
                ThreadLocalRandom.current().nextLong(
                        1, Long.MAX_VALUE
                );

        // Crear el modelo del MBR.
        long inicioParticion =
                distribucion.getInicioSuperbloque();

        RegistroMBR mbr = new RegistroMBR(
                identificadorDisco,
                configuracion.getTamanoDiscoBytes(),
                inicioParticion,
                configuracion.getTamanoDiscoBytes()
                        - inicioParticion
        );

        // Crear el modelo del superbloque.
        Superbloque superbloque = new Superbloque(
                configuracion,
                distribucion.getInicioBitmap(),
                distribucion.getInicioMetadatos(),
                distribucion.getInicioDatos(),
                distribucion.getCantidadBloquesDatos()
        );

        // Preparar las estructuras binarias antes
        // de crear fisicamente el archivo.
        byte[] datosMBR =
                SerializadorMBR.serializar(mbr);

        byte[] datosSuperbloque =
                SerializadorSuperbloque.serializar(superbloque);

        // Evita sobrescribir un disco existente.
        Files.createFile(ruta);

        RandomAccessFile acceso = null;

        try {

            acceso = new RandomAccessFile(
                    ruta.toFile(), "rw"
            );

            // Establecer la capacidad del disco virtual.
            acceso.setLength(
                    configuracion.getTamanoDiscoBytes()
            );

            // Escribir el MBR en su posicion fisica.
            acceso.seek(distribucion.getInicioMBR());
            acceso.write(datosMBR);

            // Escribir el superbloque.
            acceso.seek(
                    distribucion.getInicioSuperbloque()
            );
            acceso.write(datosSuperbloque);

            // Solicitar sincronizacion de los datos.
            acceso.getFD().sync();

            return new DiscoVirtual(
                    ruta, acceso, mbr, superbloque
            );

        } catch (IOException | RuntimeException e) {

            if (acceso != null) {
                try {
                    acceso.close();
                } catch (IOException cierreError) {
                    e.addSuppressed(cierreError);
                }
            }

            try {
                Files.deleteIfExists(ruta);
            } catch (IOException borradoError) {
                e.addSuppressed(borradoError);
            }

            throw e;
        }
    }

    /**
     * Abre un disco existente y recupera sus estructuras.
     */
    public static DiscoVirtual abrir(
            Path rutaDisco) throws IOException {

        if (rutaDisco == null) {
            throw new IllegalArgumentException(
                    "La ruta del disco no puede ser nula."
            );
        }

        Path ruta = rutaDisco.toAbsolutePath();

        RandomAccessFile acceso =
                new RandomAccessFile(ruta.toFile(), "rw");

        try {

            long tamanoFisico = acceso.length();

            if (tamanoFisico <
                    DistribucionDisco.TAMANO_MBR
                    + DistribucionDisco.TAMANO_SUPERBLOQUE) {

                throw new IOException(
                        "El disco es demasiado pequeno."
                );
            }

            // Leer los 512 bytes del MBR.
            byte[] datosMBR =
                    new byte[SerializadorMBR.TAMANO];

            acceso.seek(0);
            acceso.readFully(datosMBR);

            RegistroMBR mbr =
                    SerializadorMBR.deserializar(datosMBR);

            // Leer el superbloque.
            byte[] datosSuperbloque =
                    new byte[SerializadorSuperbloque.TAMANO];

            acceso.seek(DistribucionDisco.TAMANO_MBR);
            acceso.readFully(datosSuperbloque);

            Superbloque superbloque =
                    SerializadorSuperbloque.deserializar(
                            datosSuperbloque
                    );

            // Verificar coherencia entre el archivo
            // fisico, el MBR y el superbloque.
            validarEstructuras(
                    ruta,
                    tamanoFisico,
                    mbr,
                    superbloque
            );

            return new DiscoVirtual(
                    ruta, acceso, mbr, superbloque
            );

        } catch (IOException | RuntimeException e) {

            try {
                acceso.close();
            } catch (IOException cierreError) {
                e.addSuppressed(cierreError);
            }

            throw e;
        }
    }

    /**
     * Comprueba que las estructuras recuperadas
     * coincidan con la geometria del disco.
     */
    private static void validarEstructuras(
            Path ruta,
            long tamanoFisico,
            RegistroMBR mbr,
            Superbloque superbloque) throws IOException {

        if (mbr.getTamanoDiscoBytes() != tamanoFisico
                || superbloque.getTamanoDiscoBytes()
                != tamanoFisico) {

            throw new IOException(
                    "El tamano fisico no coincide con los metadatos."
            );
        }

        if (mbr.getInicioParticion()
                != DistribucionDisco.TAMANO_MBR
                || mbr.getTamanoParticion()
                != tamanoFisico - DistribucionDisco.TAMANO_MBR) {

            throw new IOException(
                    "La particion del MBR es inconsistente."
            );
        }

        ConfiguracionDisco configuracion =
                new ConfiguracionDisco(
                        ruta.getFileName().toString(),
                        superbloque.getNombreFileSystem(),
                        superbloque.getTamanoDiscoBytes(),
                        superbloque.getTamanoBloqueBytes(),
                        superbloque.getEstrategiaAsignacion()
                );

        ValidadorConfiguracion.validar(configuracion);

        DistribucionDisco distribucion =
                new DistribucionDisco(configuracion);

        if (superbloque.getInicioBitmap()
                    != distribucion.getInicioBitmap()
                || superbloque.getInicioMetadatos()
                    != distribucion.getInicioMetadatos()
                || superbloque.getInicioDatos()
                    != distribucion.getInicioDatos()
                || superbloque.getCantidadBloquesDatos()
                    != distribucion.getCantidadBloquesDatos()) {

            throw new IOException(
                    "La distribucion almacenada es inconsistente."
            );
        }
    }

    public Path getRuta() {
        return ruta;
    }

    public RegistroMBR getMbr() {
        return mbr;
    }

    public Superbloque getSuperbloque() {
        return superbloque;
    }

    /**
     * Lee una cantidad de bytes desde una posicion fisica
     * del disco virtual.
     *
     * No permite leer fuera de los limites del archivo.
     */
    public synchronized byte[] leer(
            long posicion, int cantidad) throws IOException {

        validarRango(posicion, cantidad);

        byte[] datos = new byte[cantidad];

        archivo.seek(posicion);
        archivo.readFully(datos);

        return datos;
    }

    /**
     * Escribe bytes en una posicion fisica del disco virtual.
     *
     * No permite escribir fuera de los limites del archivo.
     */
    public synchronized void escribir(
            long posicion, byte[] datos) throws IOException {

        if (datos == null) {
            throw new IllegalArgumentException(
                    "Los datos no pueden ser nulos.");
        }

        validarRango(posicion, datos.length);

        archivo.seek(posicion);
        archivo.write(datos);

        // Sincronizar los cambios con el archivo fisico.
        archivo.getFD().sync();
    }

    /**
     * Verifica los limites de lectura y escritura.
     */
    private void validarRango(
            long posicion, int cantidad) {

        long tamano = superbloque.getTamanoDiscoBytes();

        if (posicion < 0 || cantidad < 0
                || posicion > tamano
                || cantidad > tamano - posicion) {

            throw new IndexOutOfBoundsException(
                    "Acceso fuera de los limites del disco.");
        }
    }

    /**
     * Cierra el archivo fisico del disco virtual.
     */
    @Override
    public synchronized void close() throws IOException {
        archivo.close();
    }
}