package org.fanchuo.avroexcel.infer;

import org.apache.avro.Schema;

public class Schemas {
  private Schemas() {}

  static final Schema DATETIME =
      new Schema.Parser().parse("{\"type\": \"long\", \"logicalType\": \"timestamp-millis\"}");
  static final Schema LOCAL_DATETIME =
      new Schema.Parser()
          .parse("{\"type\": \"long\", \"logicalType\": \"local-timestamp-millis\"}");
}
