package almacenamiento;

import config.ConfiguracionDisco;
import config.ValidadorConfiguracion;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import modelo.archivos.FCB;
import modelo.disco.DistribucionDisco;
import modelo.disco.DistribucionMetadatos;
import modelo.seguridad.Grupo;
import modelo.seguridad.SeguridadContrasenas;
import modelo.seguridad.Usuario;

/**
 * Inicializa un disco virtual nuevo con sus estructuras basicas.
 *
 * Solo crea volumenes nuevos: nunca reformatea ni sobrescribe un .fs existente.
 * El montaje, los comandos interactivos y la administracion de usuarios son
 * responsabilidades de otros componentes.
 */
public final class FormateadorDisco {

    private FormateadorDisco() {
    }

    /**
     * Crea un volumen nuevo, el grupo y usuario root y los tres FCB iniciales.
     * La contrasena recibida pertenece al llamador: NO se modifica aqui.
     *
     * @return ruta del archivo .fs recien creado
     */
    public static Path formatear(
            ConfiguracionDisco configuracion,
            char[] contrasenaRoot) throws IOException {

        ValidadorConfiguracion.validar(configuracion);

        if (contrasenaRoot == null || contrasenaRoot.length == 0) {
            throw new IllegalArgumentException(
                    "Debe proporcionar una contrasena para root.");
        }

        // Validar la capacidad antes de crear un archivo fisico.
        DistribucionDisco distribucion = new DistribucionDisco(configuracion);
        DistribucionMetadatos metadatos =
                new DistribucionMetadatos(
                        distribucion.getInicioMetadatos(),
                        distribucion.getTamanoMetadatos());

        if (metadatos.getCantidadGrupos() < 1
                || metadatos.getCantidadUsuarios() < 1
                || metadatos.getCantidadFCB() < 3) {
            throw new IllegalArgumentException(
                    "El disco no tiene espacio para las estructuras iniciales.");
        }

        Path ruta = Path.of(configuracion.getNombreDisco());
        // DiscoVirtual.crear tambien rechaza archivos existentes (proteccion
        // efectiva frente a carreras); este chequeo solo mejora el mensaje.
        if (Files.exists(ruta)) {
            throw new IOException(
                    "El disco ya existe y no sera sobrescrito: " + ruta);
        }

        byte[] sal = null;
        byte[] hash = null;
        boolean discoCreado = false;
        boolean completado = false;

        try {
            sal = SeguridadContrasenas.generarSal();
            hash = SeguridadContrasenas.generarHash(contrasenaRoot, sal);

            Grupo grupoRoot = new Grupo(0, "root");
            Usuario usuarioRoot = new Usuario(
                    0, "root", "Administrador del Sistema", 0,
                    hash, sal, "/user/root", true);

            FCB raiz = FCB.crearRaiz(0, 0);
            FCB user = FCB.crearDirectorio(1, 0, "user", 0, 0, 0755);
            FCB homeRoot = FCB.crearDirectorio(2, 1, "root", 0, 0, 0700);

            try (DiscoVirtual disco = DiscoVirtual.crear(configuracion)) {
                discoCreado = true;

                // Un volumen recien creado parte con tablas y bitmap en cero.
                GestorMetadatos gestorMetadatos = new GestorMetadatos(disco);
                GestorFCB gestorFCB = new GestorFCB(disco);

                if (gestorMetadatos.leerGrupo(0) != null
                        || gestorMetadatos.leerUsuario(0) != null
                        || gestorFCB.leerFCB(0) != null) {
                    throw new IOException(
                            "Los registros iniciales no estan vacios.");
                }

                gestorMetadatos.guardarGrupo(0, grupoRoot);
                gestorMetadatos.guardarUsuario(0, usuarioRoot);
                gestorFCB.guardarFCB(0, raiz);
                gestorFCB.guardarFCB(1, user);
                gestorFCB.guardarFCB(2, homeRoot);
            }

            completado = true;
            return ruta;

        } finally {
            if (sal != null) {
                Arrays.fill(sal, (byte) 0);
            }
            if (hash != null) {
                Arrays.fill(hash, (byte) 0);
            }
            // Un formateo incompleto no debe dejar un .fs aparentemente valido.
            // Solo borramos si esta invocacion alcanzo a crear el archivo.
            if (discoCreado && !completado) {
                Files.deleteIfExists(ruta);
            }
        }
    }
}