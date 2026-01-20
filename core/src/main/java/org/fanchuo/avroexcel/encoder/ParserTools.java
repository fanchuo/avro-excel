package org.fanchuo.avroexcel.encoder;

import java.util.*;
import java.util.function.Predicate;
import org.apache.avro.Schema;
import org.apache.poi.ss.util.CellAddress;
import org.fanchuo.avroexcel.excelutil.CompositeErrorMessage;
import org.fanchuo.avroexcel.excelutil.ErrorMessage;
import org.fanchuo.avroexcel.excelutil.FormatErrorMessage;
import org.fanchuo.avroexcel.recordgeometry.RecordGeometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserTools {
  private static final Logger LOGGER = LoggerFactory.getLogger(ParserTools.class);

  private ParserTools() {
    super();
  }

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

  public static ExcelRecord visitNull(List<Schema> schemas, CellAddress address) {
    LOGGER.debug("visitNull : schemas: {}", schemas);
    Map<Schema, Object> candidates = new HashMap<>();
    Map<Schema, ErrorMessage> failures = new HashMap<>();
    for (Schema schema : schemas) {
      CollectionTypes collectionTypes = ParserTools.collectTypes(schema);
      if (collectionTypes.nullable) {
        candidates.put(schema, null);
      } else if (collectionTypes.listable) {
        candidates.put(schema, Collections.emptyList());
      } else if (collectionTypes.mappable) {
        candidates.put(schema, Collections.emptyMap());
      } else {
        failures.put(schema, new FormatErrorMessage("Not a nullable data", address));
      }
    }
    LOGGER.debug("return null - {}", candidates);
    return new ExcelRecord(candidates, failures, RecordGeometry.ATOM, true);
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
    ParserResult attempt(T structure, Schema s, CellAddress address);
  }

  public static <T> ParserResult parse(
      T subRecords,
      Schema schema,
      Schema.Type sType,
      ParseAttempt<T> attempt,
      CellAddress address) {
    List<Schema> schemas = ParserTools.flatten(schema, x -> x.getType() == sType);
    CompositeErrorMessage errorMessage = new CompositeErrorMessage();
    for (Schema s : schemas) {
      ParserResult parseAttempt = attempt.attempt(subRecords, s, address);
      if (parseAttempt.errorMessage == null) return parseAttempt;
      errorMessage.add(
          new FormatErrorMessage("Cannot match schema %s", address, new SchemaReport(s)));
      errorMessage.add(parseAttempt.errorMessage);
    }
    if (errorMessage.size() == 0)
      return new ParserResult(new FormatErrorMessage("No schema of type %s", address, sType), null);
    return new ParserResult(errorMessage, null);
  }
}
