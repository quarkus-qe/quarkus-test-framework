package io.quarkus.qe.database.mysql;

import java.util.List;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.panache.common.Sort;

@Path("/book")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookResource {

    @GET
    public List<Book> getAll() {
        return Book.listAll(Sort.by("title"));
    }

    @GET
    @Path("/{id}")
    public Book get(@PathParam("id") Long id) {
        Book book = Book.findById(id);
        if (book == null) {
            throw new NotFoundException("book '" + id + "' not found");
        }
        return book;
    }

    @POST
    @Transactional
    public Response create(@Valid Book book) {
        if (book.id != null) {
            throw new ClientErrorException("unexpected ID in request", ValidationExceptionMapper.UNPROCESSABLE_ENTITY);
        }

        book.persist();
        return Response.ok(book).status(Response.Status.CREATED).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Book update(@PathParam("id") Long id, @Valid Book newBook) {
        Book book = Book.findById(id);
        if (book == null) {
            throw new NotFoundException("book '" + id + "' not found");
        }

        book.title = newBook.title;
        book.author = newBook.author;
        return book;
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        Book book = Book.findById(id);
        if (book == null) {
            throw new NotFoundException("book '" + id + "' not found");
        }
        book.delete();
        return Response.noContent().build();
    }
}
