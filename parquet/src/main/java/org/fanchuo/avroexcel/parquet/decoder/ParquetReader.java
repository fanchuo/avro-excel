package org.fanchuo.avroexcel.parquet.decoder;

import java.io.IOException;
import java.nio.file.Path;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.io.LocalInputFile;
import org.fanchuo.avroexcel.core.decoder.GenericRecordIterator;

public class ParquetReader implements GenericRecordIterator {
  private final org.apache.parquet.hadoop.ParquetReader<GenericRecord> parquetReader;
  private GenericRecord lastRead;

  public ParquetReader(Path avroFile, GenericData genericData) throws IOException {
    this.parquetReader =
        AvroParquetReader.<GenericRecord>builder(new LocalInputFile(avroFile))
            .withDataModel(genericData)
            .build();
    this.lastRead = this.parquetReader.read();
  }

  @Override
  public GenericRecord readRecord() throws IOException {
    if (lastRead == null) return null;
    GenericRecord rec = this.lastRead;
    this.lastRead = this.parquetReader.read();
    return rec;
  }

  @Override
  public void close() throws IOException {
    this.parquetReader.close();
  }

  @Override
  public Schema getSchema() {
    return this.lastRead.getSchema();
  }
}
