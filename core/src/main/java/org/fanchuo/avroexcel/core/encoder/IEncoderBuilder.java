package org.fanchuo.avroexcel.core.encoder;

import java.io.OutputStream;

public interface IEncoderBuilder {
  GenericRecordConsumer build(OutputStream outputStream);
}
