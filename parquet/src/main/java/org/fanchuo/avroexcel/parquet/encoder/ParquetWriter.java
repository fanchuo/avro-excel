package org.fanchuo.avroexcel.parquet.encoder;

import java.io.IOException;
import java.nio.file.Path;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.LocalOutputFile;
import org.fanchuo.avroexcel.core.encoder.GenericRecordConsumer;

public class ParquetWriter implements GenericRecordConsumer {

  private final org.apache.parquet.hadoop.ParquetWriter<GenericRecord> parquetWriter;

  public ParquetWriter(Schema schema, GenericData genericData, Path parquetOutputFile)
      throws IOException {
    this.parquetWriter =
        AvroParquetWriter.<GenericRecord>builder(new LocalOutputFile(parquetOutputFile))
            .withSchema(schema)
            .withDataModel(genericData)
            .withValidation(true)
            .withCompressionCodec(CompressionCodecName.SNAPPY)
            .build();
  }

  @Override
  public void writeRecord(GenericRecord record) throws IOException {
    this.parquetWriter.write(record);
  }

  @Override
  public void close() throws IOException {
    this.parquetWriter.close();
  }
}
