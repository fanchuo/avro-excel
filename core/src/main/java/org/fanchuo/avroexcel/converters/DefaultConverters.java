package org.fanchuo.avroexcel.converters;

public class DefaultConverters implements IConverters {

  private final IExcelFieldParser excelFieldParser;
  private final IExcelFieldFormater excelFieldFormater;

  public DefaultConverters() {
    this.excelFieldParser = makeExcelFieldParser();
    this.excelFieldFormater = makeExcelFieldFormater();
  }

  private IExcelFieldParser makeExcelFieldParser() {
    return new ExcelFieldParser();
  }

  private IExcelFieldFormater makeExcelFieldFormater() {
    return new ExcelFieldFormater();
  }

  @Override
  public IExcelFieldFormater getExcelFieldFormater() {
    return excelFieldFormater;
  }

  @Override
  public IExcelFieldParser getExcelFieldParser() {
    return excelFieldParser;
  }
}
