package org.fanchuo.avroexcel.encoder;

import java.io.OutputStream;
import org.apache.avro.generic.GenericData;

public class AvroEncoderBuilder implements IEncoderBuilder {
  private final GenericData genericData;

  public AvroEncoderBuilder(GenericData genericData) {
    this.genericData = genericData;
  }

  @Override
  public GenericRecordConsumer build(OutputStream outputStream) {
    return new AvroWriter(this.genericData, outputStream);
  }
}
