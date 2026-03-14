package org.fanchuo.avroexcel.decoder;

import java.io.IOException;
import java.io.InputStream;
import org.apache.avro.generic.GenericData;

public class AvroDecoderBuilder implements IDecoderBuilder {
  private final GenericData genericData;

  public AvroDecoderBuilder(GenericData genericData) {
    this.genericData = genericData;
  }

  @Override
  public GenericRecordIterator build(InputStream inputStream) throws IOException {
    return new AvroReader(inputStream, this.genericData);
  }
}
