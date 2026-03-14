package org.fanchuo.avroexcel.encoder;

import java.io.OutputStream;

public interface IEncoderBuilder {
  GenericRecordConsumer build(OutputStream outputStream);
}
