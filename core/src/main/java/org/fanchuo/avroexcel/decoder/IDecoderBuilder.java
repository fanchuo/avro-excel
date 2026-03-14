package org.fanchuo.avroexcel.decoder;

import java.io.IOException;
import java.io.InputStream;

public interface IDecoderBuilder {
  GenericRecordIterator build(InputStream inputStream) throws IOException;
}
