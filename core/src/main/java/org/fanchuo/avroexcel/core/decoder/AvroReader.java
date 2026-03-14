package org.fanchuo.avroexcel.core.decoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;

public class AvroReader implements GenericRecordIterator {
  private final DataFileStream<GenericRecord> dataFileStream;

  public AvroReader(InputStream avroStream, GenericData genericData) throws IOException {
    this.dataFileStream =
        new DataFileStream<>(avroStream, new GenericDatumReader<>(null, null, genericData));
  }

  @Override
  public GenericRecord readRecord() {
    Iterator<GenericRecord> it = this.dataFileStream.iterator();
    if (it.hasNext()) return it.next();
    return null;
  }

  @Override
  public void close() throws IOException {
    this.dataFileStream.close();
  }

  @Override
  public Schema getSchema() {
    return this.dataFileStream.getSchema();
  }
}
