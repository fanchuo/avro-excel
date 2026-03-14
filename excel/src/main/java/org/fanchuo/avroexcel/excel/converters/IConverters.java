package org.fanchuo.avroexcel.excel.converters;

public interface IConverters {
  IExcelFieldParser getExcelFieldParser();

  IExcelFieldFormater getExcelFieldFormater();
}
