package tech.kood.rental.repository.exception;

import java.sql.SQLException;

public class ForeignKeyViolationException extends ConstraintViolationException {
    public ForeignKeyViolationException(SQLException cause) {
        super("A referenced record does not exist.", cause);
    }
}