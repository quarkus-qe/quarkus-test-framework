package io.quarkus.qe.books;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoSchema;

@ProtoSchema(includeClasses = Book.class, schemaPackageName = "book_sample")
interface BookSchema extends GeneratedSchema {
}
