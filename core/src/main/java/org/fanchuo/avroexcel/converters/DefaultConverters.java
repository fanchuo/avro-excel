package org.fanchuo.avroexcel.converters;

import org.apache.avro.data.TimeConversions;
import org.apache.avro.generic.GenericData;

public class DefaultConverters implements IConverters {
  @Override
  public GenericData makeGenericData() {
    GenericData genericData = GenericData.get();
    genericData.addLogicalTypeConversion(new TimeConversions.DateConversion());
    genericData.addLogicalTypeConversion(new TimeConversions.TimestampMillisConversion());
    genericData.addLogicalTypeConversion(new TimeConversions.TimeMicrosConversion());
    genericData.addLogicalTypeConversion(new TimeConversions.TimeMillisConversion());
    genericData.addLogicalTypeConversion(new TimeConversions.TimestampMicrosConversion());
    genericData.addLogicalTypeConversion(new TimeConversions.LocalTimestampMicrosConversion());
    genericData.addLogicalTypeConversion(new TimeConversions.LocalTimestampNanosConversion());
    genericData.addLogicalTypeConversion(new TimeConversions.LocalTimestampMillisConversion());
    return genericData;
  }

  @Override
  public IExcelFieldParser makeExcelFieldParser() {
    return new ExcelFieldParser();
  }

  @Override
  public IExcelFieldFormater makeExcelFieldFormater() {
    return new ExcelFieldFormater();
  }
}
