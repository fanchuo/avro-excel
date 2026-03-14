package org.fanchuo.avroexcel.core.avroutil;

import java.io.*;
import org.apache.avro.generic.GenericRecord;
import org.fanchuo.avroexcel.core.decoder.DecoderSchemaException;
import org.fanchuo.avroexcel.core.decoder.GenericRecordIterator;
import org.fanchuo.avroexcel.core.decoder.IDecoderBuilder;
import org.fanchuo.avroexcel.core.encoder.GenericRecordConsumer;
import org.fanchuo.avroexcel.core.encoder.IEncoderBuilder;

public class Convertion {
  public static void convert(
      InputStream inputStream,
      OutputStream outputStream,
      IDecoderBuilder decoderBuilder,
      IEncoderBuilder encoderBuilder)
      throws IOException, DecoderSchemaException {
    try (GenericRecordConsumer recordConsumer = encoderBuilder.build(outputStream);
        GenericRecordIterator recordIterator = decoderBuilder.build(inputStream)) {
      GenericRecord record;
      recordConsumer.declareSchema(recordIterator.getSchema());
      while ((record = recordIterator.readRecord()) != null) {
        recordConsumer.writeRecord(record);
      }
    }
  }

  public static void convert(
      File inputFile,
      File outputFile,
      IDecoderBuilder decoderBuilder,
      IEncoderBuilder encoderBuilder)
      throws IOException, DecoderSchemaException {
    try (InputStream inputStream = new FileInputStream(inputFile);
        OutputStream outputStream = new FileOutputStream(outputFile)) {
      convert(inputStream, outputStream, decoderBuilder, encoderBuilder);
    }
  }
}
