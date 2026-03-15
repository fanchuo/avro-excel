package org.fanchuo.avroexcel.core.encoder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import org.apache.avro.Schema;

public interface IEncoderBuilder {
  GenericRecordConsumer build(Schema schema, OutputStream outputStream) throws IOException;

  GenericRecordConsumer build(Schema schema, Path outputFile) throws IOException;
}
