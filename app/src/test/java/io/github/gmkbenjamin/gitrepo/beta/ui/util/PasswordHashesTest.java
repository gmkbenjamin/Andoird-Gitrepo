package io.github.gmkbenjamin.gitrepo.beta.ui.util;
import org.junit.Test;
import static org.junit.Assert.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordHashesTest {
    @Test public void saltsAreUniqueAndRoundTrip() {
        String one = PasswordHashes.hash("correct horse battery");
        String two = PasswordHashes.hash("correct horse battery");
        assertNotEquals(one, two);
        assertTrue(one.startsWith("pbkdf2_sha256$600000$"));
        assertTrue(PasswordHashes.verify("correct horse battery", one));
        assertFalse(PasswordHashes.verify("wrong", one));
        assertFalse(PasswordHashes.needsUpgrade(one));
    }
    @Test public void legacyOnlyMigratesAfterCorrectPassword() {
        String old = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8";
        assertTrue(PasswordHashes.verify("password", old));
        assertFalse(PasswordHashes.verify("wrong", old));
        assertTrue(PasswordHashes.needsUpgrade(old));
        String upgraded = PasswordHashes.hash("password");
        assertTrue(PasswordHashes.verify("password", upgraded));
        assertFalse(PasswordHashes.needsUpgrade(upgraded));
    }
    @Test public void matchesIndependentJcaPbkdf2() throws Exception {
        String value = PasswordHashes.hash("password");
        String[] fields = value.split("\\$");
        byte[] salt = new byte[16];
        for (int i = 0; i < salt.length; i++) salt[i] = (byte) Integer.parseInt(fields[2].substring(i*2, i*2+2), 16);
        byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(new PBEKeySpec("password".toCharArray(), salt, 600000, 256)).getEncoded();
        StringBuilder hex = new StringBuilder();
        for (byte b : key) hex.append(String.format("%02x", b & 255));
        assertEquals(fields[3], hex.toString());
    }
    @Test public void unicodeAndWhitespaceArePreserved() {
        String value = PasswordHashes.hash("  密碼 🔐  ");
        assertTrue(PasswordHashes.verify("  密碼 🔐  ", value));
        assertFalse(PasswordHashes.verify("密碼 🔐", value));
    }
    @Test public void malformedHashesFailClosed() {
        for (String value : new String[]{"", "broken", "pbkdf2_sha256$0$x$x", "pbkdf2_sha256$2147483647$x$x",
                "pbkdf2_sha256$600000$bad$bad", "unknown$600000$x$x", "pbkdf2_sha256$-1$x$x"}) {
            assertFalse(PasswordHashes.verify("password", value));
        }
        assertFalse(PasswordHashes.verify(null, "broken"));
        assertFalse(PasswordHashes.verify("password", null));
    }
}
