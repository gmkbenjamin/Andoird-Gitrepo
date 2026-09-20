package io.github.gmkbenjamin.gitrepo.beta.ui.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;

/** Versioned salted PBKDF2; legacy SHA-256 is accepted only to migrate on login. */
public final class PasswordHashes {
    private static final String SCHEME = "pbkdf2_sha256";
    private static final int ITERATIONS = 600_000;
    private static final int MAX_ITERATIONS = 1_200_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final int MAX_PASSWORD_CHARS = 4096;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHashes() { }

    public static String hash(String password) {
        if (!validPassword(password)) {
            throw new IllegalArgumentException("Password is empty or too long");
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return SCHEME + "$" + ITERATIONS + "$" + hex(salt) + "$"
                + hex(derive(password, salt, ITERATIONS));
    }

    public static boolean verify(String password, String encoded) {
        if (!validPassword(password) || encoded == null || encoded.length() > 256) {
            return false;
        }
        if (isLegacy(encoded)) {
            byte[] supplied = password.getBytes(StandardCharsets.UTF_8);
            try {
                return MessageDigest.isEqual(
                        decodeHex(encoded, HASH_BYTES),
                        MessageDigest.getInstance("SHA-256").digest(supplied));
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException("SHA-256 unavailable", impossible);
            } finally {
                Arrays.fill(supplied, (byte) 0);
            }
        }
        String[] fields = encoded.split("\\$", -1);
        if (fields.length != 4 || !SCHEME.equals(fields[0])) {
            return false;
        }
        try {
            int rounds = parseIterations(fields[1]);
            byte[] salt = decodeHex(fields[2], SALT_BYTES);
            byte[] expected = decodeHex(fields[3], HASH_BYTES);
            byte[] actual = derive(password, salt, rounds);
            try {
                return MessageDigest.isEqual(expected, actual);
            } finally {
                Arrays.fill(actual, (byte) 0);
            }
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }

    /** Only call after successful verification. Unknown schemes are never upgraded. */
    public static boolean needsUpgrade(String encoded) {
        if (encoded == null) return false;
        if (isLegacy(encoded)) return true;
        String[] fields = encoded.split("\\$", -1);
        if (fields.length != 4 || !SCHEME.equals(fields[0])) return false;
        try {
            return parseIterations(fields[1]) < ITERATIONS;
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }

    private static int parseIterations(String value) {
        if (!value.matches("[1-9][0-9]{0,6}")) throw new IllegalArgumentException();
        int rounds = Integer.parseInt(value);
        if (rounds < 100_000 || rounds > MAX_ITERATIONS) throw new IllegalArgumentException();
        return rounds;
    }

    private static boolean validPassword(String password) {
        return password != null && !password.isEmpty() && password.length() <= MAX_PASSWORD_CHARS;
    }

    private static boolean isLegacy(String encoded) {
        return encoded.matches("[0-9a-fA-F]{64}");
    }

    private static byte[] derive(String password, byte[] salt, int rounds) {
        byte[] input = password.getBytes(StandardCharsets.UTF_8);
        try {
            // Explicit digest avoids Android provider/API-level defaults (minSdk 21).
            PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
            generator.init(input, salt, rounds);
            return ((KeyParameter) generator.generateDerivedParameters(HASH_BYTES * 8)).getKey();
        } finally {
            Arrays.fill(input, (byte) 0);
        }
    }

    private static byte[] decodeHex(String value, int size) {
        if (value.length() != size * 2 || !value.matches("[0-9a-fA-F]+")) {
            throw new IllegalArgumentException("Malformed hash encoding");
        }
        byte[] result = new byte[size];
        for (int i = 0; i < size; i++) {
            result[i] = (byte) ((Character.digit(value.charAt(i * 2), 16) << 4)
                    | Character.digit(value.charAt(i * 2 + 1), 16));
        }
        return result;
    }

    private static String hex(byte[] value) {
        char[] digits = "0123456789abcdef".toCharArray();
        char[] result = new char[value.length * 2];
        for (int i = 0; i < value.length; i++) {
            result[i * 2] = digits[(value[i] & 0xff) >>> 4];
            result[i * 2 + 1] = digits[value[i] & 0x0f];
        }
        return new String(result);
    }
}
