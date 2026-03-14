package org.fanchuo.avroexcel.encoder;

import java.io.OutputStream;
import org.fanchuo.avroexcel.converters.IExcelFieldFormater;

public class ExcelEncoderBuilder implements IEncoderBuilder {
  private final String sheetname;
  private final int col;
  private final int row;
  private final IExcelFieldFormater excelFieldFormater;

  public ExcelEncoderBuilder(
      String sheetname, int col, int row, IExcelFieldFormater excelFieldFormater) {
    this.sheetname = sheetname;
    this.col = col;
    this.row = row;
    this.excelFieldFormater = excelFieldFormater;
  }

  @Override
  public GenericRecordConsumer build(OutputStream outputStream) {
    return new ExcelWriter(
        outputStream, this.sheetname, this.excelFieldFormater, this.col, this.row);
  }
}
