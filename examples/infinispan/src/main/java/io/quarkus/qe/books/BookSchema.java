package io.quarkus.qe.books;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = Book.class, schemaPackageName = "book_sample")
interface BookSchema extends GeneratedSchema {
}
