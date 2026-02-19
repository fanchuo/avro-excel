package org.fanchuo.avroexcel.decoder;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;

public class AvroReader implements Closeable {
  private final Iterable<GenericRecord> iterable;
  private final Closeable closeable;
  private final Schema schema;

  public AvroReader(File avroFile, GenericData genericData) throws IOException {
    this(new DataFileReader<>(avroFile, new GenericDatumReader<>(null, null, genericData)));
  }

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

  public void process(Consumer<GenericRecord> consumer) {
    for (GenericRecord record : this.iterable) {
      consumer.accept(record);
    }
  }

  @Override
  public void close() throws IOException {
    if (this.closeable != null) this.closeable.close();
  }

  public Schema getSchema() {
    return schema;
  }
}
