package org.fanchuo.avroexcel.core.encoder;

import java.io.Closeable;
import java.io.IOException;
import org.apache.avro.generic.GenericRecord;

public interface GenericRecordConsumer extends Closeable {
  void writeRecord(GenericRecord record) throws IOException;
}
