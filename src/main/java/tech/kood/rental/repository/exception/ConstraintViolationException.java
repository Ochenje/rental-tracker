package tech.kood.rental.repository.exception;

import java.sql.SQLException;
import java.util.Locale;

public class ConstraintViolationException extends RuntimeException {
    public ConstraintViolationException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ConstraintViolationException from(SQLException cause) {
        String message = cause.getMessage() == null ? "" : cause.getMessage().toUpperCase(Locale.ROOT);
        if (message.contains("UNIQUE")) return new UniqueConstraintViolationException(cause);
        if (message.contains("FOREIGN KEY")) return new ForeignKeyViolationException(cause);
        if (message.contains("NOT NULL")) return new NotNullViolationException(cause);
        if (message.contains("CHECK")) return new CheckConstraintViolationException(cause);
        return new ConstraintViolationException("Database constraint was violated.", cause);
    }

    public static boolean isConstraintViolation(SQLException exception) {
        return exception.getErrorCode() == 19 ||
                (exception.getMessage() != null &&
                        exception.getMessage().toUpperCase(Locale.ROOT).contains("CONSTRAINT"));
    }
}
