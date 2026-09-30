package tech.kood.rental.repository.exception;

public class CannotOpenDatabaseException extends RuntimeException {
    public CannotOpenDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
