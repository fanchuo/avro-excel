package org.fanchuo.avroexcel.parquet.encoder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import org.apache.avro.Schema;
import org.fanchuo.avroexcel.core.avroutil.IGenericDataConf;
import org.fanchuo.avroexcel.core.encoder.GenericRecordConsumer;
import org.fanchuo.avroexcel.core.encoder.IEncoderBuilder;

public class ParquetEncoderBuilder implements IEncoderBuilder {
  private final IGenericDataConf genericDataConf;

  public ParquetEncoderBuilder(IGenericDataConf genericDataConf) {
    this.genericDataConf = genericDataConf;
  }

  @Override
  public GenericRecordConsumer build(Schema schema, OutputStream outputStream) {
    throw new UnsupportedOperationException();
  }

  @Override
  public GenericRecordConsumer build(Schema schema, Path outputFile) throws IOException {
    return new ParquetWriter(schema, this.genericDataConf.getGenericData(), outputFile);
  }
}
