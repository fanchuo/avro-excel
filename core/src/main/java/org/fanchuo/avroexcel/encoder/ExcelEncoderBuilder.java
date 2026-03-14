package org.fanchuo.avroexcel.encoder;

import java.io.OutputStream;
import org.fanchuo.avroexcel.converters.IConverters;

public class ExcelEncoderBuilder implements IEncoderBuilder {
  private final String sheetname;
  private final int col;
  private final int row;
  private final IConverters converters;

  public ExcelEncoderBuilder(String sheetname, int col, int row, IConverters converters) {
    this.sheetname = sheetname;
    this.col = col;
    this.row = row;
    this.converters = converters;
  }

  @Override
  public GenericRecordConsumer build(OutputStream outputStream) {
    return new ExcelWriter(
        outputStream, this.sheetname, this.converters.getExcelFieldFormater(), this.col, this.row);
  }
}
