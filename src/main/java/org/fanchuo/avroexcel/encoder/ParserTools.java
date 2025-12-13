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

  @FunctionalInterface
  public interface ParseAttempt<T> {
    ParserResult attempt(T structure, Schema s);
  }

  public static <T> ParserResult parse(
      T subRecords, Schema schema, Schema.Type sType, ParseAttempt<T> attempt) {
    List<Schema> schemas = ParserTools.flatten(schema, x -> x.getType() == sType);
    for (Schema s : schemas) {
      ParserResult parseAttempt = attempt.attempt(subRecords, s);
      if (parseAttempt.compatible) return parseAttempt;
    }
    return ParserResult.NOT_MATCH;
  }
}
