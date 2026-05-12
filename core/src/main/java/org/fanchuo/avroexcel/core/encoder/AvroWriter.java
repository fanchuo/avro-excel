package org.fanchuo.avroexcel.core.encoder;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;

public class AvroWriter implements GenericRecordConsumer {

  private final DataFileWriter<GenericRecord> dataFileWriter;

  public AvroWriter(Schema schema, GenericData genericData, OutputStream avroOutputStream)
      throws IOException {
    DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(schema, genericData);
    this.dataFileWriter = new DataFileWriter<>(datumWriter);
    dataFileWriter.create(schema, avroOutputStream);
  }

  @Override
  public void writeRecord(GenericRecord record) throws IOException {
    this.dataFileWriter.append(record);
  }

  @Override
  public void close() throws IOException {
    this.dataFileWriter.close();
  }
}
