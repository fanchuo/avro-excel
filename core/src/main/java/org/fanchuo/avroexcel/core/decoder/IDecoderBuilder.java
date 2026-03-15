package org.fanchuo.avroexcel.core.decoder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface IDecoderBuilder {
  GenericRecordIterator build(InputStream inputStream) throws IOException;

  GenericRecordIterator build(Path inputFile) throws IOException;
}
