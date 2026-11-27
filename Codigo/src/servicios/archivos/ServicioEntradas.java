package servicios.archivos;

import almacenamiento.GestorFCB;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import modelo.archivos.FCB;

/**
 * Operaciones persistentes sobre entradas de archivos y directorios.
 * La eliminacion de archivos con contenido delega la liberacion de
 * bloques al ServicioContenidoArchivos.
 * No gestiona permisos ni sesiones (fase de seguridad posterior).
 */
public final class ServicioEntradas {

    private final GestorFCB gestor;
    private final ServicioRutas rutas;
    private final ServicioDirectorios directorios;
    private final ServicioContenidoArchivos contenido;

    public ServicioEntradas(GestorFCB gestor, ServicioRutas rutas,
            ServicioDirectorios directorios) {
        this(gestor, rutas, directorios, null);
    }

    /** Constructor para eliminacion integral de archivos con contenido. */
    public ServicioEntradas(GestorFCB gestor, ServicioRutas rutas,
            ServicioDirectorios directorios, ServicioContenidoArchivos contenido) {
        if (gestor == null || rutas == null || directorios == null) {
            throw new IllegalArgumentException("Los servicios de archivos son obligatorios.");
        }
        this.gestor = gestor;
        this.rutas = rutas;
        this.directorios = directorios;
        this.contenido = contenido;
    }

    /** Crea un archivo vacio sin sobrescribir uno existente. */
    public synchronized FCB crearArchivo(String ruta, int actual,
            int propietario, int grupo) throws IOException {
        Destino destino = separarDestino(ruta, actual);
        comprobarNombreLibre(destino.padre.getId(), destino.nombre, -1);
        long libre = buscarLibre();
        FCB nuevo = FCB.crearArchivo((int) libre, destino.padre.getId(),
                destino.nombre, propietario, grupo, 0644);
        gestor.guardarFCB(libre, nuevo);
        return nuevo;
    }

    /**
     * Mueve o renombra un FCB conservando su ID y su referencia a bloques.
     * Si destino existe y es un directorio, mueve dentro de el con el mismo nombre.
     * En otro caso, destino representa el nombre nuevo dentro de un padre existente.
     */
    public synchronized FCB mover(String origen, String destino, int actual)
            throws IOException {
        FCB fuente = rutas.resolver(origen, actual);
        if (fuente.getId() < 3) {
            throw new IOException("No se pueden mover las entradas iniciales del sistema.");
        }

        FCB destinoExistente = buscarOpcional(destino, actual);
        FCB padre;
        String nuevoNombre;
        if (destinoExistente != null) {
            if (destinoExistente.getId() == fuente.getId()) {
                return fuente;
            }
            if (!destinoExistente.esDirectorio()) {
                throw new IOException("Ya existe el destino: " + destino);
            }
            padre = destinoExistente;
            nuevoNombre = fuente.getNombre();
        } else {
            Destino separado = separarDestino(destino, actual);
            padre = separado.padre;
            nuevoNombre = separado.nombre;
        }

        if (fuente.esDirectorio()) {
            Set<Integer> visitados = new HashSet<>();
            FCB cursor = padre;
            while (true) {
                if (!visitados.add(cursor.getId())) {
                    throw new IOException("Ciclo detectado entre directorios.");
                }
                if (cursor.getId() == fuente.getId()) {
                    throw new IOException("No se puede mover un directorio dentro de si mismo.");
                }
                if (cursor.esRaiz()) {
                    break;
                }
                FCB siguiente = gestor.leerFCB(cursor.getIdPadre());
                if (siguiente == null || !siguiente.esDirectorio()) {
                    throw new IOException("La jerarquia de directorios es inconsistente.");
                }
                cursor = siguiente;
            }
        }

        comprobarNombreLibre(padre.getId(), nuevoNombre, fuente.getId());
        if (fuente.getIdPadre() == padre.getId()
                && fuente.getNombre().equals(nuevoNombre)) {
            return fuente;
        }
        long ahora = System.currentTimeMillis();
        FCB cambiado = new FCB(
                fuente.getId(), padre.getId(), nuevoNombre, fuente.getTipo(),
                fuente.getIdPropietario(), fuente.getIdGrupo(), fuente.getPermisos(),
                fuente.getTamanoBytes(), fuente.getBloqueInicial(),
                fuente.getCantidadBloques(), fuente.getFechaCreacion(),
                ahora, fuente.getFechaAcceso(), fuente.getCantidadEnlaces());
        gestor.actualizarFCB(fuente.getId(), cambiado);
        return cambiado;
    }

    /**
     * Elimina una entrada; una carpeta no vacia requiere recursivo=true.
     * Valida todo el subarbol antes de comenzar a escribir cambios.
     */
    public synchronized int eliminar(String ruta, int actual, boolean recursivo,
            int directorioActual) throws IOException {
        FCB objetivo = rutas.resolver(ruta, actual);
        if (objetivo.getId() < 3) {
            throw new IOException("No se pueden eliminar las entradas iniciales del sistema.");
        }
        // No permitir eliminar el directorio actual ni ninguno de sus padres.
        int cursorId = directorioActual;
        Set<Integer> visitados = new HashSet<>();
        while (true) {
            if (!visitados.add(cursorId)) {
                throw new IOException("Jerarquia inconsistente: ciclo de padres.");
            }
            if (cursorId == objetivo.getId()) {
                throw new IOException("No se puede eliminar el directorio actual ni sus padres.");
            }
            FCB cursor = gestor.leerFCB(cursorId);
            if (cursor == null || !cursor.esDirectorio()) {
                throw new IOException("Directorio actual invalido.");
            }
            if (cursor.esRaiz()) {
                break;
            }
            cursorId = cursor.getIdPadre();
        }

        List<FCB> orden = new ArrayList<>();
        reunirPostorden(objetivo, recursivo, orden, new HashSet<>());
        // Validar el arbol completo antes de borrar cualquier entrada.
        for (FCB fcb : orden) {
            if (fcb.getCantidadEnlaces() != 1) {
                throw new IOException("No se pueden eliminar entradas con varios enlaces: "
                        + fcb.getNombre());
            }
            boolean tieneDatos = fcb.getCantidadBloques() != 0
                    || fcb.getBloqueInicial() != FCB.SIN_BLOQUE
                    || fcb.getTamanoBytes() != 0;
            if (fcb.esDirectorio() && tieneDatos) {
                throw new IOException("Directorio con bloques inesperados: "
                        + fcb.getNombre());
            }
            if (fcb.esArchivo() && tieneDatos && contenido == null) {
                throw new IOException("No hay un gestor de contenido para liberar bloques: "
                        + fcb.getNombre());
            }
        }
        // Orden postorden: archivos primero y directorios despues.
        // No es una transaccion atomica; ante un error de I/O puede
        // quedar parte del arbol eliminada.
        for (FCB fcb : orden) {
            if (fcb.esArchivo() && contenido != null
                    && (fcb.getCantidadBloques() != 0
                        || fcb.getBloqueInicial() != FCB.SIN_BLOQUE
                        || fcb.getTamanoBytes() != 0)) {
                contenido.vaciar(fcb.getId());
            }
            gestor.eliminarFCB(fcb.getId());
        }
        return orden.size();
    }

    private void reunirPostorden(FCB fcb, boolean recursivo,
            List<FCB> resultado, Set<Integer> visitados) throws IOException {
        if (!visitados.add(fcb.getId())) {
            throw new IOException("Ciclo de directorios detectado.");
        }
        if (fcb.esDirectorio()) {
            List<FCB> hijos = directorios.listarHijos(fcb.getId());
            if (!recursivo && !hijos.isEmpty()) {
                throw new IOException("El directorio no esta vacio; use rm -R.");
            }
            for (FCB hijo : hijos) {
                if (hijo.getId() < 3) {
                    throw new IOException("La operacion incluye un directorio protegido.");
                }
                reunirPostorden(hijo, recursivo, resultado, visitados);
            }
        }
        resultado.add(fcb);
    }

    /** Recorre recursivamente un directorio, sin alterar el disco. */
    public void listarRecursivo(FCB directorio, String rutaActual,
            List<String> lineas) throws IOException {
        listarRecursivo(directorio, rutaActual, lineas, new HashSet<>());
    }

    private void listarRecursivo(FCB dir, String ruta,
            List<String> lineas, Set<Integer> visitados) throws IOException {
        if (!dir.esDirectorio()) {
            throw new IOException("No es un directorio: " + ruta);
        }
        if (!visitados.add(dir.getId())) {
            throw new IOException("Ciclo de directorios detectado.");
        }
        lineas.add(ruta + ":");
        List<FCB> hijos = directorios.listarHijos(dir.getId());
        for (FCB hijo : hijos) {
            lineas.add("  " + hijo.getNombre() + (hijo.esDirectorio() ? "/" : ""));
        }
        for (FCB hijo : hijos) {
            if (hijo.esDirectorio()) {
                String subruta = ruta.equals("/") ? "/" + hijo.getNombre()
                        : ruta + "/" + hijo.getNombre();
                listarRecursivo(hijo, subruta, lineas, visitados);
            }
        }
    }

    /** Devuelve null solo cuando la ruta no existe, sin ocultar errores de I/O. */
    private FCB buscarOpcional(String ruta, int actual) throws IOException {
        // Buscar padres con resolver; un destino existente puede ser la raiz.
        try {
            return rutas.resolver(ruta, actual);
        } catch (IOException ex) {
            if (ex.getMessage() != null
                    && ex.getMessage().startsWith("No existe la ruta:")) {
                return null;
            }
            throw ex;
        }
    }

    private Destino separarDestino(String ruta, int actual) throws IOException {
        if (ruta == null || ruta.isBlank()) {
            throw new IllegalArgumentException("La ruta no puede estar vacia.");
        }
        String normalizada = ruta.trim();
        if (normalizada.endsWith("/")) {
            throw new IllegalArgumentException("El nombre del archivo no puede terminar en /.");
        }
        int separador = normalizada.lastIndexOf('/');
        String nombre = normalizada.substring(separador + 1);
        validarNombre(nombre);
        String padreRuta = separador < 0 ? "."
                : separador == 0 ? "/" : normalizada.substring(0, separador);
        FCB padre = rutas.resolver(padreRuta, actual);
        if (!padre.esDirectorio()) {
            throw new IOException("El padre no es un directorio.");
        }
        return new Destino(padre, nombre);
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.equals(".")
                || nombre.equals("..") || nombre.contains("/")
                || nombre.indexOf('\0') >= 0
                || nombre.getBytes(StandardCharsets.UTF_8).length > FCB.MAX_NOMBRE_BYTES) {
            throw new IllegalArgumentException("Nombre de archivo o directorio invalido.");
        }
    }

    private void comprobarNombreLibre(int padre, String nombre, int ignorar)
            throws IOException {
        for (FCB hijo : directorios.listarHijos(padre)) {
            if (hijo.getId() != ignorar && hijo.getNombre().equals(nombre)) {
                throw new IOException("Ya existe una entrada llamada: " + nombre);
            }
        }
    }

    private long buscarLibre() throws IOException {
        long capacidad = gestor.getDistribucion().getCantidadFCB();
        for (long i = 3; i < capacidad && i <= Integer.MAX_VALUE; i++) {
            if (gestor.leerFCB(i) == null) {
                return i;
            }
        }
        throw new IOException("La tabla de FCB esta llena.");
    }

    private static final class Destino {
        private final FCB padre;
        private final String nombre;

        private Destino(FCB padre, String nombre) {
            this.padre = padre;
            this.nombre = nombre;
        }
    }
}