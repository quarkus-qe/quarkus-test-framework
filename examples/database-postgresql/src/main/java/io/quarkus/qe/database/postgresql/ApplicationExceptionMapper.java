package io.quarkus.qe.database.postgresql;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;

@Provider
public class ApplicationExceptionMapper implements ExceptionMapper<Exception> {
    @Override
    public Response toResponse(Exception exception) {
        int code = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        if (exception instanceof WebApplicationException) {
            code = ((WebApplicationException) exception).getResponse().getStatus();
        }

        return Response.status(code)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ObjectMapper().createObjectNode()
                        .put("code", code)
                        .put("error", exception.getMessage())
                        .toString())
                .build();
    }
}
