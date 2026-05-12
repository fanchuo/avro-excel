package org.fanchuo.avroexcel.excel.encoder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.avro.Schema;
import org.fanchuo.avroexcel.core.encoder.GenericRecordConsumer;
import org.fanchuo.avroexcel.core.encoder.IEncoderBuilder;
import org.fanchuo.avroexcel.excel.converters.IConverters;

public class ExcelEncoderBuilder implements IEncoderBuilder {
  private final String sheetName;
  private final int col;
  private final int row;
  private final IConverters converters;

  private static String makeSheetName(String sheetName) {
    if (sheetName == null) return "tab";
    return sheetName;
  }

  public ExcelEncoderBuilder(String sheetName, int col, int row, IConverters converters) {
    this.sheetName = makeSheetName(sheetName);
    this.col = col;
    this.row = row;
    this.converters = converters;
  }

  @Override
  public GenericRecordConsumer build(Schema schema, OutputStream outputStream) {
    return new ExcelWriter(
        schema,
        outputStream,
        this.sheetName,
        this.converters.getExcelFieldFormater(),
        this.col,
        this.row);
  }

  @Override
  public GenericRecordConsumer build(Schema schema, Path outputFile) throws IOException {
    return this.build(schema, new BufferedOutputStream(Files.newOutputStream(outputFile)));
  }
}
