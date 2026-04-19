package uk.ac.westminster.smartcampus.exception;

import javax.ws.rs.WebApplicationException;

public class SensorUnavailableException extends WebApplicationException {
    public SensorUnavailableException(String message) {
        super(message);
    }
}
