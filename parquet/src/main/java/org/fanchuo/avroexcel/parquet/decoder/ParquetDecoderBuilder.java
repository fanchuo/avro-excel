package org.fanchuo.avroexcel.parquet.decoder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import org.fanchuo.avroexcel.core.decoder.GenericRecordIterator;
import org.fanchuo.avroexcel.core.decoder.IDecoderBuilder;

public class ParquetDecoderBuilder implements IDecoderBuilder {
  @Override
  public GenericRecordIterator build(Path inputFile) throws IOException {
    return new ParquetReader(inputFile);
  }

  @Override
  public GenericRecordIterator build(InputStream inputStream) {
    throw new UnsupportedOperationException();
  }
}
