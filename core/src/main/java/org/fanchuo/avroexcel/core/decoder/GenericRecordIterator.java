package org.fanchuo.avroexcel.core.decoder;

import java.io.Closeable;
import java.io.IOException;
import org.apache.avro.Schema;

public interface GenericRecordIterator extends Closeable {
  Schema getSchema();

  ValidatedGenericRecord readRecord() throws IOException;
}
