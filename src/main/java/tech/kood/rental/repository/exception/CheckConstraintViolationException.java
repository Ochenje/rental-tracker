package tech.kood.rental.repository.exception;

import java.sql.SQLException;

public class CheckConstraintViolationException extends ConstraintViolationException {
    public CheckConstraintViolationException(SQLException cause) {
        super("A value violates a database check constraint.", cause);
    }
}