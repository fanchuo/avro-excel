package org.fanchuo.avroexcel.core.decoder;

import java.io.Closeable;
import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;

public interface GenericRecordIterator extends Closeable {
  Schema getSchema();

  GenericRecord readRecord() throws DecoderSchemaException, IOException;
}
