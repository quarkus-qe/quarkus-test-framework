#!/bin/sh
rm -r target/classes src/main/java
mkdir -p target/classes
mkdir -p src/main/java

for j in '..' '../examples'; do
  for i in $(find $j -regex .*target/classes); do
    cp -r $i/* target/classes/
  done
  for i in $(find $j -regex .*src/main/java); do
    cp -r $i/* src/main/java/
  done
done

#delete classes from examples
rm -r target/classes/io/quarkus/qe
rm -r target/classes/org/acme

#delete classes from quarkus cli
rm -r target/classes/io/quarkus/cli

#we don't care about classes in the 'graal' package, because they are only used in native image generation
find target/classes/ -name graal -exec rm -r {} \;

#antlr generated code
rm -r target/classes/io/quarkus/panacheql/internal

#we don't care about the document processor
rm -r target/classes/io/quarkus/annotation/processor/generate_doc

#needed to make sure the script always succeeds
echo "complete"
