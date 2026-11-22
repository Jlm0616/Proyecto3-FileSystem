package modelo.seguridad;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Proporciona operaciones de proteccion y verificacion
 * de contrasenas mediante PBKDF2WithHmacSHA256.
 *
 * No administra usuarios ni accede al disco.
 */
public final class SeguridadContrasenas {

    private static final String ALGORITMO =
            "PBKDF2WithHmacSHA256";

    private static final int ITERACIONES = 600000;
    private static final int TAMANO_SAL = 16;
    private static final int TAMANO_HASH_BITS = 256;

    private static final SecureRandom RANDOM =
            new SecureRandom();

    private SeguridadContrasenas() {
        // Clase utilitaria.
    }

    /**
     * Genera una sal aleatoria de 16 bytes.
     */
    public static byte[] generarSal() {

        byte[] sal = new byte[TAMANO_SAL];
        RANDOM.nextBytes(sal);

        return sal;
    }

    /**
     * Genera el hash de una contrasena.
     */
    public static byte[] generarHash(
            char[] contrasena,
            byte[] sal) {

        validarDatos(contrasena, sal);

        PBEKeySpec especificacion = new PBEKeySpec(
                contrasena,
                sal,
                ITERACIONES,
                TAMANO_HASH_BITS
        );

        try {

            SecretKeyFactory fabrica =
                    SecretKeyFactory.getInstance(ALGORITMO);

            return fabrica.generateSecret(
                    especificacion).getEncoded();

        } catch (Exception e) {

            throw new IllegalStateException(
                    "No fue posible generar el hash.",
                    e
            );

        } finally {
            especificacion.clearPassword();
        }
    }

    /**
     * Comprueba una contrasena contra su hash almacenado.
     */
    public static boolean verificar(
            char[] contrasena,
            byte[] sal,
            byte[] hashAlmacenado) {

        if (hashAlmacenado == null
                || hashAlmacenado.length
                != TAMANO_HASH_BITS / 8) {

            return false;
        }

        byte[] hashCalculado =
                generarHash(contrasena, sal);

        try {

            return MessageDigest.isEqual(
                    hashCalculado,
                    hashAlmacenado
            );

        } finally {
            Arrays.fill(hashCalculado, (byte) 0);
        }
    }

    /**
     * Valida los parametros necesarios para generar
     * una contrasena protegida.
     */
    private static void validarDatos(
            char[] contrasena,
            byte[] sal) {

        if (contrasena == null
                || contrasena.length == 0) {

            throw new IllegalArgumentException(
                    "La contrasena no puede estar vacia.");
        }

        if (sal == null
                || sal.length != TAMANO_SAL) {

            throw new IllegalArgumentException(
                    "La sal debe contener exactamente "
                    + TAMANO_SAL + " bytes.");
        }
    }
}