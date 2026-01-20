package org.fanchuo.avroexcel.encoder;

import java.util.stream.Collectors;
import org.apache.avro.Schema;

public class SchemaReport {
  private final Schema schema;

  public SchemaReport(Schema schema) {
    this.schema = schema;
  }

  @Override
  public String toString() {
    switch (schema.getType()) {
      case RECORD:
        return String.format(
            "RECORD %s %s",
            schema.getName(),
            schema.getFields().stream().map(Schema.Field::name).collect(Collectors.toList()));
      case ARRAY:
        return String.format("ARRAY %s", new SchemaReport(schema.getElementType()));
      case MAP:
        return String.format("MAP %s", new SchemaReport(schema.getValueType()));
      case UNION:
        return schema.getTypes().stream()
            .map(SchemaReport::new)
            .collect(Collectors.toList())
            .toString();
      default:
        return schema.toString();
    }
  }
}
