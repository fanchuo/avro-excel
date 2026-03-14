package org.fanchuo.avroexcel.encoder;

import java.io.Closeable;
import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;

public interface GenericRecordConsumer extends Closeable {
  void declareSchema(Schema schema) throws IOException;

  void writeRecord(GenericRecord record) throws IOException;
}
