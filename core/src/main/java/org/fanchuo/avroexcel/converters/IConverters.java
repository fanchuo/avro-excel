package org.fanchuo.avroexcel.converters;

public interface IConverters {
  IExcelFieldParser getExcelFieldParser();

  IExcelFieldFormater getExcelFieldFormater();
}
