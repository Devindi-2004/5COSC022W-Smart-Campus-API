package uk.ac.westminster.smartcampus.exception;

import javax.ws.rs.WebApplicationException;

public class LinkedResourceNotFoundException extends WebApplicationException {
    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}
