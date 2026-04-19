package uk.ac.westminster.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {
    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        Map<String, String> body = new HashMap<>();
        body.put("error", "Unprocessable Entity");
        body.put("message", exception.getMessage());
        return Response.status(422) // JAX-RS Response.Status doesn't have UNPROCESSABLE_ENTITY directly in old versions, use 422 explicitly
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
