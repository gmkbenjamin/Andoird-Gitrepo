package io.github.gmkbenjamin.gitrepo.beta.ui.util;

import java.io.File;
import java.io.IOException;

/** Canonical containment for old Gitrepo archives, including legacy /gitrepo entries. */
public final class SafeZip {
    private SafeZip() { }
    public static File resolveEntry(File target, String name) throws IOException {
        if (name == null || name.isEmpty() || name.indexOf('\0') >= 0 || name.indexOf('\\') >= 0) {
            throw new IOException("Invalid archive path");
        }
        // The legacy writer produced /gitrepo/... rather than relative entry names.
        if (name.startsWith("/gitrepo/")) name = name.substring(1);
        if (name.equals("/gitrepo")) name = "gitrepo";
        if (new File(name).isAbsolute() || name.indexOf(':') >= 0) {
            throw new IOException("Absolute archive path");
        }
        for (String component : name.split("/")) {
            if (component.equals("..")) throw new IOException("Archive path traversal");
        }
        File lexicalRoot = new File(target.getCanonicalFile(), "gitrepo").getAbsoluteFile();
        File root = lexicalRoot.getCanonicalFile();
        if (!root.equals(lexicalRoot)) {
            throw new IOException("Symlinked repository restore root");
        }
        File result = new File(target, name).getCanonicalFile();
        if (!result.equals(root) && !result.getPath().startsWith(root.getPath() + File.separator)) {
            throw new IOException("Archive entry escapes repository directory");
        }
        return result;
    }
}
