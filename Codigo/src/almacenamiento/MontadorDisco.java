package almacenamiento;

import java.io.IOException;
import java.nio.file.Path;
import modelo.archivos.FCB;
import modelo.disco.DistribucionMetadatos;
import modelo.disco.Superbloque;
import modelo.seguridad.Grupo;
import modelo.seguridad.Usuario;

/**
 * Monta un volumen ya formateado y comprueba sus estructuras esenciales.
 *
 * Mantiene abierto el DiscoVirtual hasta cerrar el montaje. No modifica el
 * volumen ni solicita contrasenas (eso corresponde a la autenticacion).
 */
public final class MontadorDisco implements AutoCloseable {

    private final DiscoVirtual disco;
    private final GestorMetadatos gestorMetadatos;
    private final GestorFCB gestorFCB;
    private final GestorEspacioLibre gestorEspacioLibre;
    private final Usuario root;
    private final Grupo grupoRoot;
    private final FCB directorioRaiz;
    private final FCB directorioUser;
    private final FCB directorioRoot;

    private MontadorDisco(DiscoVirtual disco) throws IOException {
        this.disco = disco;
        try {
            this.gestorMetadatos = new GestorMetadatos(disco);
            this.gestorFCB = new GestorFCB(disco);
            this.gestorEspacioLibre = new GestorEspacioLibre(disco);

            Superbloque superbloque = disco.getSuperbloque();
            DistribucionMetadatos distribucion = gestorFCB.getDistribucion();
            if (distribucion.getCantidadGrupos() < 1
                    || distribucion.getCantidadUsuarios() < 1
                    || distribucion.getCantidadFCB() < 3
                    || distribucion.getFinMetadatos() != superbloque.getInicioDatos()) {
                throw new IOException("Region de metadatos incompatible.");
            }

            this.grupoRoot = gestorMetadatos.leerGrupo(0);
            this.root = gestorMetadatos.leerUsuario(0);
            this.directorioRaiz = gestorFCB.leerFCB(0);
            this.directorioUser = gestorFCB.leerFCB(1);
            this.directorioRoot = gestorFCB.leerFCB(2);

            if (grupoRoot == null || grupoRoot.getId() != 0
                    || !"root".equals(grupoRoot.getNombre())) {
                throw new IOException("Grupo root ausente o inconsistente.");
            }
            if (root == null || root.getId() != 0
                    || !"root".equals(root.getNombreUsuario())
                    || root.getIdGrupoPrincipal() != grupoRoot.getId()
                    || !root.esAdministrador()
                    || !"/user/root".equals(root.getDirectorioHome())) {
                throw new IOException("Usuario root ausente o inconsistente.");
            }
            validarDirectorio(directorioRaiz, 0, -1, "/");
            validarDirectorio(directorioUser, 1, 0, "user");
            validarDirectorio(directorioRoot, 2, 1, "root");
            if (directorioRaiz.getIdPropietario() != 0
                    || directorioUser.getIdPropietario() != 0
                    || directorioRoot.getIdPropietario() != 0
                    || directorioRaiz.getIdGrupo() != 0
                    || directorioUser.getIdGrupo() != 0
                    || directorioRoot.getIdGrupo() != 0
                    || directorioRoot.getPermisos() != 0700) {
                throw new IOException("Propiedad o permisos iniciales inconsistentes.");
            }
            if (gestorEspacioLibre.getCantidadBloques()
                    != superbloque.getCantidadBloquesDatos()) {
                throw new IOException("Cantidad de bloques del bitmap incompatible.");
            }
            long ocupados = gestorEspacioLibre.contarBloquesOcupados();
            if (ocupados < 0 || ocupados > gestorEspacioLibre.getCantidadBloques()) {
                throw new IOException("Contador del bitmap invalido.");
            }
        } catch (IOException | RuntimeException error) {
            // La fabrica montar se encarga de cerrar el disco al fallar.
            throw new IOException("No se pudo validar el montaje: "
                    + error.getMessage(), error);
        }
    }

    private static void validarDirectorio(
            FCB fcb, int id, int padre, String nombre) throws IOException {
        if (fcb == null || !fcb.esDirectorio() || fcb.getId() != id
                || fcb.getIdPadre() != padre || !nombre.equals(fcb.getNombre())) {
            throw new IOException("Directorio inicial ausente o invalido: " + nombre);
        }
    }

    /** Abre y valida el disco; el llamador debe cerrar el montaje. */
    public static MontadorDisco montar(Path ruta) throws IOException {
        if (ruta == null) {
            throw new IllegalArgumentException("La ruta no puede ser nula.");
        }
        DiscoVirtual disco = DiscoVirtual.abrir(ruta);
        try {
            return new MontadorDisco(disco);
        } catch (IOException | RuntimeException error) {
            try {
                disco.close();
            } catch (IOException cierre) {
                error.addSuppressed(cierre);
            }
            throw error;
        }
    }

    public DiscoVirtual getDisco() {
        return disco;
    }

    public GestorMetadatos getGestorMetadatos() {
        return gestorMetadatos;
    }

    public GestorFCB getGestorFCB() {
        return gestorFCB;
    }

    public GestorEspacioLibre getGestorEspacioLibre() {
        return gestorEspacioLibre;
    }

    public Usuario getRoot() {
        return root;
    }

    public Grupo getGrupoRoot() {
        return grupoRoot;
    }

    public FCB getDirectorioRaiz() {
        return directorioRaiz;
    }

    public FCB getDirectorioUser() {
        return directorioUser;
    }

    public FCB getDirectorioRoot() {
        return directorioRoot;
    }

    @Override
    public void close() throws IOException {
        disco.close();
    }
}