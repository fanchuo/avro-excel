package org.fanchuo.avroexcel.decoder;

import java.io.IOException;
import java.io.InputStream;
import org.fanchuo.avroexcel.avroutil.IGenericDataConf;

public class AvroDecoderBuilder implements IDecoderBuilder {
  private final IGenericDataConf genericDataConf;

  public AvroDecoderBuilder(IGenericDataConf genericDataConf) {
    this.genericDataConf = genericDataConf;
  }

  @Override
  public GenericRecordIterator build(InputStream inputStream) throws IOException {
    return new AvroReader(inputStream, this.genericDataConf.getGenericData());
  }
}
