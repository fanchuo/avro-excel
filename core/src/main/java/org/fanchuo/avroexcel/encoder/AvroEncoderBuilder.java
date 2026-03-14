package org.fanchuo.avroexcel.encoder;

import java.io.OutputStream;
import org.fanchuo.avroexcel.avroutil.IGenericDataConf;

public class AvroEncoderBuilder implements IEncoderBuilder {
  private final IGenericDataConf genericDataConf;

  public AvroEncoderBuilder(IGenericDataConf genericDataConf) {
    this.genericDataConf = genericDataConf;
  }

  @Override
  public GenericRecordConsumer build(OutputStream outputStream) {
    return new AvroWriter(this.genericDataConf.getGenericData(), outputStream);
  }
}
