package org.fanchuo.avroexcel.core.decoder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.fanchuo.avroexcel.core.avroutil.IGenericDataConf;

public class AvroDecoderBuilder implements IDecoderBuilder {
  private final IGenericDataConf genericDataConf;

  public AvroDecoderBuilder(IGenericDataConf genericDataConf) {
    this.genericDataConf = genericDataConf;
  }

  @Override
  public GenericRecordIterator build(InputStream inputStream) throws IOException {
    return new AvroReader(inputStream, this.genericDataConf.getGenericData());
  }

  @Override
  public GenericRecordIterator build(Path inputFile) throws IOException {
    return this.build(new BufferedInputStream(Files.newInputStream(inputFile)));
  }
}
