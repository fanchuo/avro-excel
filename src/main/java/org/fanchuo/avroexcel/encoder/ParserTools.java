package org.fanchuo.avroexcel.encoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import org.apache.avro.Schema;

public class ParserTools {
  public static List<Schema> flatten(Schema schema, Predicate<Schema> predicate) {
    return flatten(Collections.singletonList(schema), predicate);
  }

  public static List<Schema> flatten(List<Schema> schemas, Predicate<Schema> predicate) {
    List<Schema> output = new ArrayList<>();
    collect(schemas, predicate, output);
    return output;
  }

  private static void collect(
      List<Schema> schemas, Predicate<Schema> predicate, List<Schema> output) {
    for (Schema schema : schemas) {
      if (schema.getType() == Schema.Type.UNION) {
        collect(schema.getTypes(), predicate, output);
      } else if (predicate.test(schema)) {
        output.add(schema);
      }
    }
  }

  public static CollectionTypes collectTypes(Schema schema) {
    CollectionTypes collectionTypes = new CollectionTypes();
    collectTypes(Collections.singletonList(schema), collectionTypes);
    return collectionTypes;
  }

  private static void collectTypes(List<Schema> schemas, CollectionTypes output) {
    for (Schema schema : schemas) {
      switch (schema.getType()) {
        case UNION:
          collectTypes(schema.getTypes(), output);
          break;
        case NULL:
          output.nullable = true;
          break;
        case ARRAY:
          output.listable = true;
          break;
        case MAP:
          output.mappable = true;
          break;
      }
    }
  }

  @FunctionalInterface
  public interface ParseAttempt<T> {
    ParserResult attempt(T structure, Schema s);
  }

  public static <T> ParserResult parse(
      T subRecords, Schema schema, Schema.Type sType, ParseAttempt<T> attempt) {
    List<Schema> schemas = ParserTools.flatten(schema, x -> x.getType() == sType);
    String errorMessage = String.format("No schema of type %s", sType);
    for (Schema s : schemas) {
      ParserResult parseAttempt = attempt.attempt(subRecords, s);
      if (parseAttempt.errorMessage == null) return parseAttempt;
      errorMessage = parseAttempt.errorMessage;
    }
    return new ParserResult(errorMessage, null);
  }
}
