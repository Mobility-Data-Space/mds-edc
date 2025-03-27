package io.nexyo.edp.extensions.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;


@Provider
public class EdpExceptionMapper implements ExceptionMapper<EdpException> {

    /**
     * Constructs a new EdpException with the specified cause.
     * @param exception the exception to be mapped
     * @return a Response object containing the exception message
     */
    @Override
    public Response toResponse(EdpException exception) {
        return Response.status(500)
                .entity("{\"error\": \"" + exception.getMessage() + "\"}")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

}
