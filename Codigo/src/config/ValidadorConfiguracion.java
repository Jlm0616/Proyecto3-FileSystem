
package config;

/**
 * Valida la configuración de un disco virtual.
 *
 * No crea archivos ni modifica configuraciones.
 */
public final class ValidadorConfiguracion {

    private ValidadorConfiguracion() {
        // Clase utilitaria: no necesita instancias.
    }

    public static void validar(ConfiguracionDisco config) {

        if (config == null) {
            throw new IllegalArgumentException(
                    "La configuracion no puede ser nula.");
        }

        validarNombreDisco(config.getNombreDisco());
        validarNombreFileSystem(config.getNombreFileSystem());
        validarTamanoDisco(config.getTamanoDiscoBytes());
        validarTamanoBloque(config.getTamanoBloqueBytes());

        if (config.getEstrategiaAsignacion() == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar una estrategia de asignacion.");
        }

        if (config.getTamanoDiscoBytes()
                / config.getTamanoBloqueBytes() < 16) {

            throw new IllegalArgumentException(
                    "El disco debe contener al menos 16 bloques teoricos.");
        }
    }

    private static void validarNombreDisco(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del disco no puede estar vacio.");
        }

        if (!nombre.matches("[a-zA-Z0-9_-]+\\.fs")) {
            throw new IllegalArgumentException(
                    "El disco debe tener un nombre valido y extension .fs.");
        }
    }

    private static void validarNombreFileSystem(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del File System no puede estar vacio.");
        }

        if (!nombre.matches("[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "El nombre del File System contiene caracteres invalidos.");
        }
    }

    private static void validarTamanoDisco(long tamano) {

        if (tamano <= 0) {
            throw new IllegalArgumentException(
                    "El tamano del disco debe ser mayor que cero.");
        }
    }

    private static void validarTamanoBloque(int tamano) {

        if (tamano <= 0) {
            throw new IllegalArgumentException(
                    "El tamano del bloque debe ser mayor que cero.");
        }
    }
}
