package io.quarkus.qe.database.postgresql;

import java.util.List;

import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
