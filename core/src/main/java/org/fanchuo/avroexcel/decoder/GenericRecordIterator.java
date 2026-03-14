package org.fanchuo.avroexcel.decoder;

import java.io.Closeable;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;

public interface GenericRecordIterator extends Closeable {
  Schema getSchema();

  GenericRecord readRecord() throws DecoderSchemaException;
}
