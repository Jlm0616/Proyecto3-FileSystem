
package consola;

import config.ConfiguracionDisco;
import config.ConfiguracionDisco.EstrategiaAsignacion;
import config.ValidadorConfiguracion;
import java.util.Scanner;

/**
 * Solicita al usuario los parametros necesarios
 * para configurar un disco virtual.
 *
 * No crea archivos ni ejecuta el formateo.
 */
public final class LectorConfiguracionDisco {

    private final Scanner scanner;

    public LectorConfiguracionDisco(Scanner scanner) {

        if (scanner == null) {
            throw new IllegalArgumentException(
                    "El lector de entrada no puede ser nulo.");
        }

        this.scanner = scanner;
    }

    /**
     * Solicita y valida la configuracion del disco.
     */
    public ConfiguracionDisco solicitarConfiguracion() {

        System.out.println();
        System.out.println("=== CONFIGURACION DEL DISCO ===");

        String nombreDisco = solicitarTexto(
                "Nombre del disco (.fs): ");

        String nombreFS = solicitarTexto(
                "Nombre del File System: ");

        long tamanoDisco = solicitarLong(
                "Tamano del disco (bytes): ");

        int tamanoBloque = solicitarEntero(
                "Tamano del bloque (bytes): ");

        EstrategiaAsignacion estrategia =
                solicitarEstrategia();

        ConfiguracionDisco configuracion =
                new ConfiguracionDisco(
                        nombreDisco,
                        nombreFS,
                        tamanoDisco,
                        tamanoBloque,
                        estrategia
                );

        ValidadorConfiguracion.validar(configuracion);

        return configuracion;
    }

    private String solicitarTexto(String mensaje) {

        while (true) {

            System.out.print(mensaje);

            String valor = scanner.nextLine().trim();

            if (!valor.isEmpty()) {
                return valor;
            }

            System.out.println(
                    "ERROR: El valor no puede estar vacio.");
        }
    }

    private long solicitarLong(String mensaje) {

        while (true) {

            String entrada = solicitarTexto(mensaje);

            try {

                long valor = Long.parseLong(entrada);

                if (valor > 0) {
                    return valor;
                }

            } catch (NumberFormatException e) {
                // Mostrar el mensaje de validacion.
            }

            System.out.println(
                    "ERROR: Introduzca un numero entero positivo.");
        }
    }

    private int solicitarEntero(String mensaje) {

        while (true) {

            String entrada = solicitarTexto(mensaje);

            try {

                int valor = Integer.parseInt(entrada);

                if (valor > 0) {
                    return valor;
                }

            } catch (NumberFormatException e) {
                // Mostrar el mensaje de validacion.
            }

            System.out.println(
                    "ERROR: Introduzca un numero entero positivo.");
        }
    }

    private EstrategiaAsignacion solicitarEstrategia() {

        while (true) {

            System.out.println();
            System.out.println("Estrategia de asignacion:");
            System.out.println("1. CONTIGUA");
            System.out.println("2. ENLAZADA");
            System.out.println("3. INDEXADA");

            System.out.print("Seleccione una opcion (1-3): ");

            String opcion = scanner.nextLine().trim();

            switch (opcion) {

                case "1":
                    return EstrategiaAsignacion.CONTIGUA;

                case "2":
                    return EstrategiaAsignacion.ENLAZADA;

                case "3":
                    return EstrategiaAsignacion.INDEXADA;

                default:
                    System.out.println(
                            "ERROR: Seleccione una opcion valida.");
            }
        }
    }
}