package org.fanchuo.avroexcel.converters;

import org.apache.avro.generic.GenericData;

public interface IConverters {
  GenericData getGenericData();

  IExcelFieldParser getExcelFieldParser();

  IExcelFieldFormater getExcelFieldFormater();
}
