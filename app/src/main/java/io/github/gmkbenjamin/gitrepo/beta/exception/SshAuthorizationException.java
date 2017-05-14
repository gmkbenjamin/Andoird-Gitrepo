package io.github.gmkbenjamin.gitrepo.beta.exception;

public class SshAuthorizationException extends Exception {
    private static final long serialVersionUID = 20120101L;

    public SshAuthorizationException(String message) {
        super(message);
    }

    public SshAuthorizationException(String message, Throwable e) {
        super(message, e);
    }
}
