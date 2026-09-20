package io.github.gmkbenjamin.gitrepo.beta.ui.util;
import java.io.File;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

public class SafeZipTest {
    @Test public void acceptsLegacyAndRelativeRepositoryPaths() throws Exception {
        File base = new File(System.getProperty("java.io.tmpdir"));
        assertEquals(new File(base, "gitrepo/repositories/example.git/config").getCanonicalFile(),
                SafeZip.resolveEntry(base, "/gitrepo/repositories/example.git/config"));
        assertEquals(new File(base, "gitrepo/test").getCanonicalFile(), SafeZip.resolveEntry(base, "gitrepo/test"));
    }
    @Test public void rejectsEscapes() throws Exception {
        File base = new File(System.getProperty("java.io.tmpdir"));
        for (String path : new String[]{"../secret", "gitrepo/../../secret", "/etc/passwd", "gitrepo/../secret",
                "gitrepo_evil/test", "C:/secret", "gitrepo\\..\\secret", "gitrepo/\0secret"}) {
            try { SafeZip.resolveEntry(base, path); fail(path); } catch (IOException expected) { }
        }
    }
}
