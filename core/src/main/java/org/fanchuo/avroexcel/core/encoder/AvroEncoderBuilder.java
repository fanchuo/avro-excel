package org.fanchuo.avroexcel.core.encoder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.avro.Schema;
import org.fanchuo.avroexcel.core.avroutil.IGenericDataConf;

public class AvroEncoderBuilder implements IEncoderBuilder {
  private final IGenericDataConf genericDataConf;

  public AvroEncoderBuilder(IGenericDataConf genericDataConf) {
    this.genericDataConf = genericDataConf;
  }

  @Override
  public GenericRecordConsumer build(Schema schema, OutputStream outputStream) throws IOException {
    return new AvroWriter(schema, this.genericDataConf.getGenericData(), outputStream);
  }

  @Override
  public GenericRecordConsumer build(Schema schema, Path outputFile) throws IOException {
    return this.build(schema, new BufferedOutputStream(Files.newOutputStream(outputFile)));
  }
}
