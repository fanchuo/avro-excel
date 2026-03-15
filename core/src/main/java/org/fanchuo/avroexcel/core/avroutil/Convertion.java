package org.fanchuo.avroexcel.core.avroutil;

import java.io.*;
import java.nio.file.Path;
import org.apache.avro.generic.GenericRecord;
import org.fanchuo.avroexcel.core.decoder.DecoderSchemaException;
import org.fanchuo.avroexcel.core.decoder.GenericRecordIterator;
import org.fanchuo.avroexcel.core.decoder.IDecoderBuilder;
import org.fanchuo.avroexcel.core.encoder.GenericRecordConsumer;
import org.fanchuo.avroexcel.core.encoder.IEncoderBuilder;

public class Convertion {
  private Convertion() {}

  public static void convert(
      InputStream inputStream,
      OutputStream outputStream,
      IDecoderBuilder decoderBuilder,
      IEncoderBuilder encoderBuilder)
      throws IOException, DecoderSchemaException {
    try (GenericRecordIterator recordIterator = decoderBuilder.build(inputStream);
        GenericRecordConsumer recordConsumer =
            encoderBuilder.build(recordIterator.getSchema(), outputStream)) {
      GenericRecord record;
      while ((record = recordIterator.readRecord()) != null) {
        recordConsumer.writeRecord(record);
      }
    }
  }

  public static void convert(
      InputStream inputStream,
      Path outputFile,
      IDecoderBuilder decoderBuilder,
      IEncoderBuilder encoderBuilder)
      throws IOException, DecoderSchemaException {
    try (GenericRecordIterator recordIterator = decoderBuilder.build(inputStream);
        GenericRecordConsumer recordConsumer =
            encoderBuilder.build(recordIterator.getSchema(), outputFile)) {
      GenericRecord record;
      while ((record = recordIterator.readRecord()) != null) {
        recordConsumer.writeRecord(record);
      }
    }
  }

  public static void convert(
      Path inputFile,
      OutputStream outputStream,
      IDecoderBuilder decoderBuilder,
      IEncoderBuilder encoderBuilder)
      throws IOException, DecoderSchemaException {
    try (GenericRecordIterator recordIterator = decoderBuilder.build(inputFile);
        GenericRecordConsumer recordConsumer =
            encoderBuilder.build(recordIterator.getSchema(), outputStream)) {
      GenericRecord record;
      while ((record = recordIterator.readRecord()) != null) {
        recordConsumer.writeRecord(record);
      }
    }
  }

  public static void convert(
      Path inputFile,
      Path outputFile,
      IDecoderBuilder decoderBuilder,
      IEncoderBuilder encoderBuilder)
      throws IOException, DecoderSchemaException {
    try (GenericRecordIterator recordIterator = decoderBuilder.build(inputFile);
        GenericRecordConsumer recordConsumer =
            encoderBuilder.build(recordIterator.getSchema(), outputFile)) {
      GenericRecord record;
      while ((record = recordIterator.readRecord()) != null) {
        recordConsumer.writeRecord(record);
      }
    }
  }
}
