package io.quarkus.qe.database.postgresql;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

// workaround for Quarkus providing its own NotFoundExceptionMapper
// which is more specific than our ApplicationExceptionMapper
@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {
    @Override
    public Response toResponse(NotFoundException exception) {
        return new ApplicationExceptionMapper().toResponse(exception);
    }
}
