package org.fanchuo.avroexcel.core.avroutil;

import java.io.*;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.fanchuo.avroexcel.core.decoder.GenericRecordIterator;
import org.fanchuo.avroexcel.core.decoder.IDecoderBuilder;
import org.fanchuo.avroexcel.core.decoder.ValidatedGenericRecord;
import org.fanchuo.avroexcel.core.encoder.GenericRecordConsumer;
import org.fanchuo.avroexcel.core.encoder.IEncoderBuilder;

public class Convertion {
  private Convertion() {}

  public static void convert(
      InputStream inputStream,
      OutputStream outputStream,
      IDecoderBuilder decoderBuilder,
      IEncoderBuilder encoderBuilder,
      Consumer<ErrorMessage> errorMessageConsumer)
      throws IOException {
    try (GenericRecordIterator recordIterator = decoderBuilder.build(inputStream);
        GenericRecordConsumer recordConsumer =
            encoderBuilder.build(recordIterator.getSchema(), outputStream)) {
      convert(recordIterator, recordConsumer, errorMessageConsumer);
    }
  }

  public static void convert(
      InputStream inputStream,
      Path outputFile,
      IDecoderBuilder decoderBuilder,
      IEncoderBuilder encoderBuilder,
      Consumer<ErrorMessage> errorMessageConsumer)
      throws IOException {
    try (GenericRecordIterator recordIterator = decoderBuilder.build(inputStream);
        GenericRecordConsumer recordConsumer =
            encoderBuilder.build(recordIterator.getSchema(), outputFile)) {
      convert(recordIterator, recordConsumer, errorMessageConsumer);
    }
  }

  public static void convert(
      Path inputFile,
      OutputStream outputStream,
      IDecoderBuilder decoderBuilder,
      IEncoderBuilder encoderBuilder,
      Consumer<ErrorMessage> errorMessageConsumer)
      throws IOException {
    try (GenericRecordIterator recordIterator = decoderBuilder.build(inputFile);
        GenericRecordConsumer recordConsumer =
            encoderBuilder.build(recordIterator.getSchema(), outputStream)) {
      convert(recordIterator, recordConsumer, errorMessageConsumer);
    }
  }

  public static void convert(
      Path inputFile,
      Path outputFile,
      IDecoderBuilder decoderBuilder,
      IEncoderBuilder encoderBuilder,
      Consumer<ErrorMessage> errorMessageConsumer)
      throws IOException {
    try (GenericRecordIterator recordIterator = decoderBuilder.build(inputFile);
        GenericRecordConsumer recordConsumer =
            encoderBuilder.build(recordIterator.getSchema(), outputFile)) {
      convert(recordIterator, recordConsumer, errorMessageConsumer);
    }
  }

  private static void convert(
      GenericRecordIterator recordIterator,
      GenericRecordConsumer recordConsumer,
      Consumer<ErrorMessage> errorMessageConsumer)
      throws IOException {
    ValidatedGenericRecord record;
    while ((record = recordIterator.readRecord()) != null) {
      if (record.isValid()) {
        recordConsumer.writeRecord(record.getGenericRecord());
      } else {
        if (errorMessageConsumer == null) {
          System.err.println(ErrorMessageDumper.dump(record.getErrorMessage()));
        } else {
          errorMessageConsumer.accept(record.getErrorMessage());
        }
      }
    }
  }
}
