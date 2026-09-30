package tech.kood.rental.repository.exception;

import java.sql.SQLException;

public class NotNullViolationException extends ConstraintViolationException {
    public NotNullViolationException(SQLException cause) {
        super("A required value is missing.", cause);
    }
}