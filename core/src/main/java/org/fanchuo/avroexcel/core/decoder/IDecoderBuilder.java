package org.fanchuo.avroexcel.core.decoder;

import java.io.IOException;
import java.io.InputStream;

public interface IDecoderBuilder {
  GenericRecordIterator build(InputStream inputStream) throws IOException;
}
