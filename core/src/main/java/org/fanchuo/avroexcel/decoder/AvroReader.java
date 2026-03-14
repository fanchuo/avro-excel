package org.fanchuo.avroexcel.decoder;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;

public class AvroReader implements GenericRecordIterator {
  private final Iterable<GenericRecord> iterable;
  private final Closeable closeable;
  private final Schema schema;

  public AvroReader(InputStream avroStream, GenericData genericData) throws IOException {
    this(new DataFileStream<>(avroStream, new GenericDatumReader<>(null, null, genericData)));
  }

  public AvroReader(DataFileStream<GenericRecord> dataFileStream) {
    this(dataFileStream, dataFileStream, dataFileStream.getSchema());
  }

  public AvroReader(Iterable<GenericRecord> iterable, Closeable closeable, Schema schema) {
    this.iterable = iterable;
    this.closeable = closeable;
    this.schema = schema;
  }

  @Override
  public GenericRecord readRecord() {
    Iterator<GenericRecord> it = this.iterable.iterator();
    if (it.hasNext()) return it.next();
    return null;
  }

  @Override
  public void close() throws IOException {
    if (this.closeable != null) this.closeable.close();
  }

  @Override
  public Schema getSchema() {
    return schema;
  }
}
