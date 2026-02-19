package org.fanchuo.avroexcel.converters;

import org.apache.avro.generic.GenericData;

public interface IConverters {
  GenericData makeGenericData();

  IExcelFieldParser makeExcelFieldParser();
}
