package io.quarkus.qe.database.mysql;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    public static final int UNPROCESSABLE_ENTITY = 422;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode errors = mapper.createArrayNode();

        for (ConstraintViolation<?> constraintViolation : exception.getConstraintViolations()) {
            errors.addObject()
                    .put("path", constraintViolation.getPropertyPath().toString())
                    .put("message", constraintViolation.getMessage());
        }

        return Response.status(UNPROCESSABLE_ENTITY)
                .type(MediaType.APPLICATION_JSON)
                .entity(mapper.createObjectNode()
                        .put("code", UNPROCESSABLE_ENTITY)
                        .set("error", errors))
                .build();
    }
}
