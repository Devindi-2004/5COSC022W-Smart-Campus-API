package uk.ac.westminster.smartcampus.exception;

import javax.ws.rs.WebApplicationException;

public class RoomNotEmptyException extends WebApplicationException {
    public RoomNotEmptyException(String message) {
        super(message);
    }
}
