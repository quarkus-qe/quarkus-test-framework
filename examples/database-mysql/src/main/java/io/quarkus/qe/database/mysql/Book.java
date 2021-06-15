package io.quarkus.qe.database.mysql;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "book")
public class Book extends PanacheEntity {
    @NotBlank(message = "book title must be set")
    public String title;

    @NotBlank(message = "book author must be set")
    public String author;
}
