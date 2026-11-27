package consola;

import almacenamiento.FormateadorDisco;
import almacenamiento.MontadorDisco;
import config.ConfiguracionDisco;
import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import modelo.archivos.FCB;
import servicios.archivos.ServicioRutas;
import servicios.archivos.ServicioDirectorios;
import servicios.archivos.ServicioEntradas;
import servicios.archivos.ServicioContenidoArchivos;
import java.util.ArrayList;

/**
 * Interfaz de linea de comandos del sistema de archivos virtual.
 * Interpreta comandos y delega la logica a sus servicios.
 */
public final class ConsolaSistemaArchivos implements AutoCloseable {

    private final Scanner entrada;
    private final LectorConfiguracionDisco lectorConfiguracion;
    private MontadorDisco montaje;
    private ServicioRutas servicioRutas;
    private ServicioDirectorios servicioDirectorios;
    private ServicioEntradas servicioEntradas;
    private ServicioContenidoArchivos servicioContenido;
    private int directorioActual;

    public ConsolaSistemaArchivos() {
        this.entrada = new Scanner(System.in);
        this.lectorConfiguracion = new LectorConfiguracionDisco(entrada);
        this.directorioActual = FCB.ID_RAIZ;
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
                    case "pwd":
                        if (!argumento.isEmpty()) {
                            System.out.println("Uso: pwd");
                        } else {
                            mostrarDirectorioActual();
                        }
                        break;
                    case "cd":
                        cambiarDirectorio(argumento);
                        break;
                    case "ls":
                        listarConOpciones(argumento);
                        break;
                    case "mkdir":
                        crearDirectorio(argumento);
                        break;
                    case "touch":
                        crearArchivo(argumento);
                        break;
                    case "rm":
                        eliminarEntrada(argumento);
                        break;
                    case "mv":
                        moverEntrada(argumento);
                        break;
                    case "cat":
                        mostrarContenido(argumento);
                        break;
                    case "write":
                        escribirContenido(argumento);
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
        System.out.println("  format           Crear un disco virtual nuevo (interactivo)");
        System.out.println("  mount archivo.fs Montar un disco existente");
        System.out.println("  info             Ver informacion del disco montado");
        System.out.println("  pwd              Mostrar directorio actual");
        System.out.println("  cd ruta          Cambiar de directorio (/, ., .., rutas absolutas o relativas)");
        System.out.println("  ls [ruta]        Listar contenido del directorio actual o indicado");
        System.out.println("  ls -R [ruta]     Listar directorios recursivamente");
        System.out.println("  mkdir ruta       Crear un directorio (el padre debe existir)");
        System.out.println("  touch ruta       Crear un archivo vacio");
        System.out.println("  rm ruta          Borrar archivo o directorio vacio");
        System.out.println("  rm -R ruta       Borrar directorio y contenido recursivamente");
        System.out.println("  mv origen destino Mover o renombrar una entrada");
        System.out.println("  cat ruta         Mostrar el contenido de un archivo");
        System.out.println("  write ruta texto Reemplazar contenido (comando provisional)");
        System.out.println("  help             Mostrar esta ayuda");
        System.out.println("  exit             Salir y cerrar el disco");
        System.out.println("Otros comandos de archivos y usuarios se agregaran posteriormente.");
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
            montar(creado);
        } finally {
            Arrays.fill(clave, '\0');
            if (confirmacion != null) {
                Arrays.fill(confirmacion, '\0');
            }
        }
    }

    private void montar(Path ruta) {
        MontadorDisco nuevo = null;
        try {
            nuevo = MontadorDisco.montar(ruta);
            ServicioRutas nuevasRutas = new ServicioRutas(nuevo.getGestorFCB());
            ServicioDirectorios nuevosDirectorios =
                    new ServicioDirectorios(nuevo.getGestorFCB());
            ServicioContenidoArchivos nuevoContenido = new ServicioContenidoArchivos(
                    nuevo.getDisco(), nuevo.getGestorFCB(), nuevo.getGestorEspacioLibre());
            ServicioEntradas nuevasEntradas = new ServicioEntradas(
                    nuevo.getGestorFCB(), nuevasRutas, nuevosDirectorios, nuevoContenido);

            // Comprobar la raiz antes de sustituir el montaje anterior.
            FCB raiz = nuevasRutas.resolver("/", FCB.ID_RAIZ);
            if (!raiz.esDirectorio()) {
                throw new IOException("La raiz del volumen no es un directorio.");
            }

            MontadorDisco anterior = montaje;
            montaje = nuevo;
            servicioRutas = nuevasRutas;
            servicioDirectorios = nuevosDirectorios;
            servicioEntradas = nuevasEntradas;
            servicioContenido = nuevoContenido;
            directorioActual = FCB.ID_RAIZ;
            nuevo = null; // La consola pasa a ser propietaria del montaje.

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
                    + montaje.getDisco().getSuperbloque().getNombreFileSystem());
            System.out.println("Usuario inicial disponible: "
                    + montaje.getRoot().getNombreUsuario());
        } catch (IOException | RuntimeException ex) {
            System.out.println("No se pudo montar el disco: " + ex.getMessage());
        } finally {
            if (nuevo != null) {
                try {
                    nuevo.close();
                } catch (IOException e) {
                    System.out.println("Aviso: error cerrando montaje fallido: "
                            + e.getMessage());
                }
            }
        }
    }

    /** Muestra la ruta absoluta del directorio actual. */
    private void mostrarDirectorioActual() throws IOException {
        exigirDiscoMontado();
        System.out.println(servicioRutas.obtenerRutaAbsoluta(directorioActual));
    }

    /** Cambia el directorio solamente si la ruta existe y es un directorio. */
    private void cambiarDirectorio(String ruta) throws IOException {
        exigirDiscoMontado();
        if (ruta.isEmpty()) {
            System.out.println("Uso: cd ruta");
            return;
        }

        FCB destino = servicioRutas.resolver(ruta, directorioActual);
        if (!destino.esDirectorio()) {
            throw new IOException("No es un directorio: " + ruta);
        }

        // El directorio actual solo cambia despues de validar el destino.
        directorioActual = destino.getId();
    }

    /** Reconoce ls y ls -R sin confundir las opciones con las rutas. */
    private void listarConOpciones(String argumento) throws IOException {
        if (argumento.equals("-R") || argumento.startsWith("-R ")) {
            String ruta = argumento.length() == 2 ? "" : argumento.substring(2).trim();
            if (ruta.startsWith("-")) {
                System.out.println("Uso: ls -R [ruta]");
                return;
            }
            exigirDiscoMontado();
            FCB inicio = servicioRutas.resolver(
                    ruta.isEmpty() ? "." : ruta, directorioActual);
            if (!inicio.esDirectorio()) {
                throw new IOException("No es un directorio: " + ruta);
            }
            List<String> lineas = new ArrayList<>();
            servicioEntradas.listarRecursivo(inicio,
                    servicioRutas.obtenerRutaAbsoluta(inicio.getId()), lineas);
            for (String linea : lineas) {
                System.out.println(linea);
            }
        } else {
            listarDirectorio(argumento);
        }
    }

    /** Lista solo los hijos directos de un directorio existente. */
    private void listarDirectorio(String ruta) throws IOException {
        exigirDiscoMontado();
        FCB destino = ruta.isEmpty()
                ? servicioRutas.resolver(".", directorioActual)
                : servicioRutas.resolver(ruta, directorioActual);

        if (!destino.esDirectorio()) {
            throw new IOException("No es un directorio: " + destino.getNombre());
        }

        List<FCB> hijos = servicioDirectorios.listarHijos(destino.getId());
        for (FCB hijo : hijos) {
            System.out.println(hijo.getNombre() + (hijo.esDirectorio() ? "/" : ""));
        }
    }

    /** Crea un directorio persistente a partir de una ruta absoluta o relativa. */
    private void crearDirectorio(String ruta) throws IOException {
        exigirDiscoMontado();
        if (ruta.isBlank()) {
            System.out.println("Uso: mkdir ruta");
            return;
        }

        // Se admite una barra final, por ejemplo mkdir documentos/.
        String normalizada = ruta.trim();
        while (normalizada.endsWith("/") && normalizada.length() > 1) {
            normalizada = normalizada.substring(0, normalizada.length() - 1);
        }
        if (normalizada.equals("/")) {
            throw new IllegalArgumentException("No se puede crear el directorio raiz.");
        }

        int separador = normalizada.lastIndexOf('/');
        String nombre = normalizada.substring(separador + 1);
        String rutaPadre;
        if (separador < 0) {
            rutaPadre = ".";
        } else if (separador == 0) {
            rutaPadre = "/";
        } else {
            rutaPadre = normalizada.substring(0, separador);
        }

        FCB padre = servicioRutas.resolver(rutaPadre, directorioActual);
        if (!padre.esDirectorio()) {
            throw new IOException("El padre no es un directorio: " + rutaPadre);
        }

        // Por ahora la consola trabaja con el usuario inicial root.
        // La validacion de permisos por sesion se implementara en la fase de seguridad.
        int propietario = montaje.getRoot().getId();
        int grupo = montaje.getRoot().getIdGrupoPrincipal();
        FCB creado = servicioDirectorios.crearDirectorio(
                padre.getId(), nombre, propietario, grupo, 0755);

        System.out.println("Directorio creado: "
                + servicioRutas.obtenerRutaAbsoluta(creado.getId()));
    }

    /** Crea un archivo vacio persistente. */
    private void crearArchivo(String ruta) throws IOException {
        exigirDiscoMontado();
        if (ruta.isBlank()) {
            System.out.println("Uso: touch ruta");
            return;
        }
        FCB creado = servicioEntradas.crearArchivo(ruta, directorioActual,
                montaje.getRoot().getId(), montaje.getRoot().getIdGrupoPrincipal());
        System.out.println("Archivo creado: "
                + servicioRutas.obtenerRutaAbsoluta(creado.getId()));
    }

    /** Borra una entrada; rm -R tambien borra arboles de archivos con datos. */
    private void eliminarEntrada(String argumento) throws IOException {
        exigirDiscoMontado();
        boolean recursivo = argumento.equals("-R") || argumento.startsWith("-R ");
        String ruta = recursivo ? argumento.substring(2).trim() : argumento;
        if (ruta.isBlank() || ruta.startsWith("-")) {
            System.out.println("Uso: rm [-R] ruta");
            return;
        }
        int total = servicioEntradas.eliminar(
                ruta, directorioActual, recursivo, directorioActual);
        System.out.println("Entradas eliminadas: " + total);
    }

    /** Muestra los bytes de un archivo interpretados como texto UTF-8. */
    private void mostrarContenido(String ruta) throws IOException {
        exigirDiscoMontado();
        if (ruta.isBlank()) {
            System.out.println("Uso: cat ruta");
            return;
        }
        FCB fcb = servicioRutas.resolver(ruta, directorioActual);
        if (!fcb.esArchivo()) {
            throw new IOException("No es un archivo: " + ruta);
        }
        byte[] datos = servicioContenido.leer(fcb.getId());
        String texto = new String(datos, StandardCharsets.UTF_8);
        System.out.print(texto);
        if (!texto.endsWith("\n")) {
            System.out.println();
        }
    }

    /** Escritura de texto UTF-8 para pruebas; note sera el editor definitivo. */
    private void escribirContenido(String argumento) throws IOException {
        exigirDiscoMontado();
        int separador = argumento.indexOf(' ');
        if (separador <= 0 || separador == argumento.length() - 1) {
            System.out.println("Uso: write ruta texto");
            return;
        }
        String ruta = argumento.substring(0, separador).trim();
        String texto = argumento.substring(separador + 1).trim();
        if (ruta.isEmpty() || texto.isEmpty()) {
            System.out.println("Uso: write ruta texto");
            return;
        }
        // Permitir comillas dobles opcionales en los extremos.
        if (texto.length() >= 2 && texto.startsWith("\"") && texto.endsWith("\"")) {
            texto = texto.substring(1, texto.length() - 1);
        }
        FCB fcb = servicioRutas.resolver(ruta, directorioActual);
        if (!fcb.esArchivo()) {
            throw new IOException("No es un archivo: " + ruta);
        }
        byte[] datos = texto.getBytes(StandardCharsets.UTF_8);
        servicioContenido.escribir(fcb.getId(), datos);
        System.out.println("Contenido guardado: " + datos.length + " bytes.");
    }

    /** Renombra o mueve conservando la identidad del FCB. */
    private void moverEntrada(String argumento) throws IOException {
        exigirDiscoMontado();
        String[] partes = argumento.trim().split("\\s+");
        if (partes.length != 2) {
            System.out.println("Uso: mv origen destino (sin espacios en las rutas)");
            return;
        }
        FCB movido = servicioEntradas.mover(partes[0], partes[1], directorioActual);
        System.out.println("Entrada ubicada en: "
                + servicioRutas.obtenerRutaAbsoluta(movido.getId()));
    }

    private void exigirDiscoMontado() {
        if (montaje == null || servicioRutas == null || servicioDirectorios == null
                || servicioEntradas == null || servicioContenido == null) {
            throw new IllegalStateException("No hay ningun disco montado.");
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
        System.out.println("Aviso: esta consola no puede ocultar la contrasena.");
        return solicitarTexto(mensaje).toCharArray();
    }

    @Override
    public void close() throws IOException {
        if (montaje != null) {
            try {
                montaje.close();
            } finally {
                montaje = null;
                servicioRutas = null;
                servicioDirectorios = null;
                servicioEntradas = null;
                servicioContenido = null;
                directorioActual = FCB.ID_RAIZ;
            }
        }
        // No se cierra System.in; pertenece al proceso principal.
    }
}