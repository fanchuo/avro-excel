package org.fanchuo.avroexcel.core.avroutil;

import org.apache.avro.Schema;

public enum Type {
  STRING(Schema.create(Schema.Type.STRING)),
  NULL(Schema.create(Schema.Type.NULL)),
  DOUBLE(Schema.create(Schema.Type.DOUBLE)),
  LOCAL_DATE(Schemas.LOCAL_DATETIME),
  TIMESTAMP(Schemas.DATETIME),
  BOOL(Schema.create(Schema.Type.BOOLEAN));

  public final Schema schema;

  Type(Schema schema) {
    this.schema = schema;
  }
}
