package consola;

import almacenamiento.FormateadorDisco;
import almacenamiento.MontadorDisco;
import config.ConfiguracionDisco;
import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

/**
 * Interfaz de linea de comandos del sistema de archivos.
 * Solo interpreta entrada/salida; delega el formateo y montaje a sus servicios.
 */
public final class ConsolaSistemaArchivos implements AutoCloseable {

    private final Scanner entrada;
    private final LectorConfiguracionDisco lectorConfiguracion;
    private MontadorDisco montaje;

    public ConsolaSistemaArchivos() {
        this.entrada = new Scanner(System.in);
        this.lectorConfiguracion = new LectorConfiguracionDisco(entrada);
    }

    /**
     * Inicia el interprete. Si se indica una ruta, intenta montar ese disco.
     */
    public void iniciar(Path rutaInicial) {
        System.out.println("======================================");
        System.out.println("       SISTEMA DE ARCHIVOS VIRTUAL    ");
        System.out.println("======================================");
        System.out.println("Escriba 'help' para ver los comandos.");

        if (rutaInicial != null) {
            montar(rutaInicial);
        }

        while (true) {
            String nombreFS = montaje == null
                    ? "sin-disco"
                    : montaje.getDisco().getSuperbloque().getNombreFileSystem();
            System.out.print(nombreFS + "> ");
            System.out.flush();

            if (!entrada.hasNextLine()) {
                System.out.println();
                return;
            }
            String linea = entrada.nextLine().trim();
            if (linea.isEmpty()) {
                continue;
            }

            int separador = linea.indexOf(' ');
            String comando = (separador < 0 ? linea : linea.substring(0, separador))
                    .toLowerCase(Locale.ROOT);
            String argumento = separador < 0 ? "" : linea.substring(separador + 1).trim();

            try {
                switch (comando) {
                    case "help":
                    case "ayuda":
                        mostrarAyuda();
                        break;
                    case "format":
                        if (!argumento.isEmpty()) {
                            System.out.println("Uso: format (los parametros se solicitan uno por uno).");
                        } else {
                            formatearInteractivo();
                        }
                        break;
                    case "mount":
                    case "abrir":
                        if (argumento.isEmpty()) {
                            System.out.println("Uso: mount nombre.fs");
                        } else {
                            montar(Path.of(argumento));
                        }
                        break;
                    case "info":
                        mostrarInformacion();
                        break;
                    case "exit":
                    case "salir":
                        System.out.println("Sistema cerrado.");
                        return;
                    default:
                        System.out.println("Comando desconocido: " + comando
                                + ". Escriba 'help'.");
                }
            } catch (IOException | IllegalArgumentException | IllegalStateException e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
    }

    private void mostrarAyuda() {
        System.out.println("Comandos disponibles:");
        System.out.println("  format          Crear un disco virtual nuevo (interactivo)");
        System.out.println("  mount archivo.fs Montar un disco existente");
        System.out.println("  info            Ver informacion del disco montado");
        System.out.println("  help            Mostrar esta ayuda");
        System.out.println("  exit            Salir y cerrar el disco");
        System.out.println("Los comandos de archivos y usuarios se agregaran en las siguientes fases.");
    }

    private void formatearInteractivo() throws IOException {
        ConfiguracionDisco configuracion = lectorConfiguracion.solicitarConfiguracion();
        Path ruta = Path.of(configuracion.getNombreDisco());
        if (Files.exists(ruta)) {
            throw new IOException("El archivo ya existe. No se sobrescribira: " + ruta);
        }

        char[] clave = solicitarContrasena("Contrasena inicial de root: ");
        char[] confirmacion = null;
        try {
            confirmacion = solicitarContrasena("Repita la contrasena: ");
            if (clave.length == 0 || !Arrays.equals(clave, confirmacion)) {
                throw new IllegalArgumentException(
                        "Las contrasenas no coinciden o estan vacias.");
            }

            Path creado = FormateadorDisco.formatear(configuracion, clave);
            System.out.println("Disco formateado correctamente: "
                    + creado.toAbsolutePath());
            // Montar sin perder el disco anterior en caso de error.
            montar(creado);
        } finally {
            Arrays.fill(clave, '\0');
            if (confirmacion != null) {
                Arrays.fill(confirmacion, '\0');
            }
        }
    }

    private void montar(Path ruta) {
        try {
            MontadorDisco nuevo = MontadorDisco.montar(ruta);
            MontadorDisco anterior = montaje;
            montaje = nuevo;
            if (anterior != null) {
                try {
                    anterior.close();
                } catch (IOException ex) {
                    System.out.println("Aviso: error cerrando el disco anterior: "
                            + ex.getMessage());
                }
            }
            System.out.println("Disco montado: " + ruta.toAbsolutePath());
            System.out.println("File System: "
                    + nuevo.getDisco().getSuperbloque().getNombreFileSystem());
            System.out.println("Usuario inicial disponible: "
                    + nuevo.getRoot().getNombreUsuario());
        } catch (IOException ex) {
            System.out.println("No se pudo montar el disco: " + ex.getMessage());
        }
    }

    private void mostrarInformacion() throws IOException {
        if (montaje == null) {
            System.out.println("No hay ningun disco montado.");
            return;
        }
        var superbloque = montaje.getDisco().getSuperbloque();
        System.out.println("Archivo: " + montaje.getDisco().getRuta());
        System.out.println("File System: " + superbloque.getNombreFileSystem());
        System.out.println("Tamano del disco: " + superbloque.getTamanoDiscoBytes()
                + " bytes");
        System.out.println("Tamano del bloque: " + superbloque.getTamanoBloqueBytes()
                + " bytes");
        System.out.println("Estrategia: " + superbloque.getEstrategiaAsignacion());
        System.out.println("Bloques de datos: " + superbloque.getCantidadBloquesDatos());
        System.out.println("Bloques ocupados: "
                + montaje.getGestorEspacioLibre().contarBloquesOcupados());
        System.out.println("Bloques libres: "
                + montaje.getGestorEspacioLibre().contarBloquesLibres());
        System.out.println("HOME root: " + montaje.getRoot().getDirectorioHome());
    }

    private String solicitarTexto(String mensaje) {
        System.out.print(mensaje);
        System.out.flush();
        if (!entrada.hasNextLine()) {
            throw new IllegalStateException("Entrada finalizada.");
        }
        return entrada.nextLine().trim();
    }

    private char[] solicitarContrasena(String mensaje) {
        Console consola = System.console();
        if (consola != null) {
            char[] clave = consola.readPassword("%s", mensaje);
            if (clave == null) {
                throw new IllegalStateException("Entrada finalizada.");
            }
            return clave;
        }
        // NetBeans normalmente no dispone de Console; alli se ve lo escrito.
        System.out.println("Aviso: esta consola no puede ocultar la contrasena.");
        return solicitarTexto(mensaje).toCharArray();
    }

    @Override
    public void close() throws IOException {
        if (montaje != null) {
            montaje.close();
            montaje = null;
        }
        // No se cierra System.in; pertenece al proceso principal.
    }
}
