package tech.kood.rental.repository.exception;

import java.sql.SQLException;

public class UniqueConstraintViolationException extends ConstraintViolationException {
    public UniqueConstraintViolationException(SQLException cause) {
        super("A value must be unique.", cause);
    }
}