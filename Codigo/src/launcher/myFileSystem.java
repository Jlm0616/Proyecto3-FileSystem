package launcher;

import consola.ConsolaSistemaArchivos;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Lanzador del Sistema de Archivos Virtual.
 *
 * Puede ejecutarse desde NetBeans como launcher.myFileSystem
 * o desde app.Main, que delega aqui el inicio normal.
 */
public final class myFileSystem {

    private myFileSystem() {
        // No se necesitan instancias de esta clase.
    }

    public static void main(String[] args) {
        iniciar(args);
    }

    public static void iniciar(String[] args) {
        if (args == null) {
            args = new String[0];
        }

        // Las pruebas de regresion se conservan temporalmente en app.Main.
        if (args.length == 1 && "--test".equals(args[0])) {
            app.Main.main(args);
            return;
        }

        if (args.length > 1) {
            System.out.println("Uso: java launcher.myFileSystem [archivo.fs]");
            System.out.println("Pruebas: java launcher.myFileSystem --test");
            return;
        }

        if (args.length == 1 && !args[0].endsWith(".fs")) {
            System.out.println("ERROR: El argumento debe ser un archivo .fs.");
            return;
        }

        Path rutaInicial = args.length == 1 ? Path.of(args[0]) : null;

        try (ConsolaSistemaArchivos consola = new ConsolaSistemaArchivos()) {
            consola.iniciar(rutaInicial);
        } catch (IOException e) {
            System.out.println("ERROR al cerrar el sistema: " + e.getMessage());
        }
    }
}
