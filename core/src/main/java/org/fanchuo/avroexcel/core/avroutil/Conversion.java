package org.fanchuo.avroexcel.core.avroutil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.apache.avro.generic.GenericRecord;
import org.fanchuo.avroexcel.core.decoder.GenericRecordIterator;
import org.fanchuo.avroexcel.core.decoder.IDecoderBuilder;
import org.fanchuo.avroexcel.core.decoder.ValidatedGenericRecord;
import org.fanchuo.avroexcel.core.encoder.GenericRecordConsumer;
import org.fanchuo.avroexcel.core.encoder.IEncoderBuilder;

public final class Conversion implements ValidationHandler, Consumer<ErrorMessage> {
  private ValidationHandler validationHandler = this;
  private Consumer<ErrorMessage> errorHandler = this;

  @Override
  public ErrorMessage validate(Object address, GenericRecord genericRecord) {
    return null;
  }

  @Override
  public void accept(ErrorMessage errorMessage) {
    System.err.println(errorMessage.toString());
  }

  public Conversion onErrors(Consumer<ErrorMessage> errorHandler) {
    this.errorHandler = errorHandler;
    return this;
  }

  public Conversion withCustomCheck(ValidationHandler validationHandler) {
    this.validationHandler = validationHandler;
    return this;
  }

  public void convert(
      InputStream inputStream,
      OutputStream outputStream,
      IDecoderBuilder decoderBuilder,
      IEncoderBuilder encoderBuilder)
      throws IOException {
    try (GenericRecordIterator recordIterator = decoderBuilder.build(inputStream);
        GenericRecordConsumer recordConsumer =
            encoderBuilder.build(recordIterator.getSchema(), outputStream)) {
      convert(recordIterator, recordConsumer);
    }
  }

  public void convert(
      InputStream inputStream,
      Path outputFile,
      IDecoderBuilder decoderBuilder,
      IEncoderBuilder encoderBuilder)
      throws IOException {
    try (GenericRecordIterator recordIterator = decoderBuilder.build(inputStream);
        GenericRecordConsumer recordConsumer =
            encoderBuilder.build(recordIterator.getSchema(), outputFile)) {
      convert(recordIterator, recordConsumer);
    }
  }

  public void convert(
      Path inputFile,
      OutputStream outputStream,
      IDecoderBuilder decoderBuilder,
      IEncoderBuilder encoderBuilder)
      throws IOException {
    try (GenericRecordIterator recordIterator = decoderBuilder.build(inputFile);
        GenericRecordConsumer recordConsumer =
            encoderBuilder.build(recordIterator.getSchema(), outputStream)) {
      convert(recordIterator, recordConsumer);
    }
  }

  public void convert(
      Path inputFile,
      Path outputFile,
      IDecoderBuilder decoderBuilder,
      IEncoderBuilder encoderBuilder)
      throws IOException {
    try (GenericRecordIterator recordIterator = decoderBuilder.build(inputFile);
        GenericRecordConsumer recordConsumer =
            encoderBuilder.build(recordIterator.getSchema(), outputFile)) {
      convert(recordIterator, recordConsumer);
    }
  }

  private void convert(GenericRecordIterator recordIterator, GenericRecordConsumer recordConsumer)
      throws IOException {
    ValidatedGenericRecord record;
    while ((record = recordIterator.readRecord()) != null) {
      if (record.isValid()) {
        ErrorMessage msg =
            validationHandler.validate(record.getAddress(), record.getGenericRecord());
        if (msg != null) {
          record = new ValidatedGenericRecord(record.getAddress(), msg);
        }
      }
      if (record.isValid()) {
        final GenericRecord r = record.getGenericRecord();
        recordConsumer.writeRecord(r);
      } else {
        errorHandler.accept(record.getErrorMessage());
      }
    }
  }
}
