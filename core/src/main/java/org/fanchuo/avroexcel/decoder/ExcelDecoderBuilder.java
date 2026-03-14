package org.fanchuo.avroexcel.decoder;

import java.io.IOException;
import java.io.InputStream;
import org.apache.avro.Schema;
import org.fanchuo.avroexcel.converters.IConverters;
import org.fanchuo.avroexcel.excelutil.ExcelSheetReader;
import org.fanchuo.avroexcel.headerinfo.HeaderInfo;
import org.fanchuo.avroexcel.headerinfo.HeaderInfoExcelReader;

public class ExcelDecoderBuilder implements IDecoderBuilder {
  private static String makeSheetname(String sheetName) {
    if (sheetName == null) return "tab";
    return sheetName;
  }

  private final IConverters converters;
  private final String sheetName;
  private final Schema schema;
  private final int col;
  private final int row;

  public ExcelDecoderBuilder(
      IConverters converters, String sheetName, Schema schema, int col, int row) {
    this.converters = converters;
    this.sheetName = makeSheetname(sheetName);
    this.schema = schema;
    this.col = col;
    this.row = row;
  }

  public ExcelDecoderBuilder(IConverters converters, Schema schema) {
    this(converters, null, schema, 0, 0);
  }

  @Override
  public GenericRecordIterator build(InputStream inputStream) throws IOException {
    ExcelSheetReader excelSheetReader = ExcelSheetReader.loadSheet(inputStream, this.sheetName);
    HeaderInfo headerInfo = HeaderInfoExcelReader.visitSheet(excelSheetReader, this.col, this.row);
    return new ExcelReader(
        this.converters.getExcelFieldParser(),
        excelSheetReader,
        this.schema,
        headerInfo,
        this.col,
        this.row + headerInfo.rowSpan);
  }
}
