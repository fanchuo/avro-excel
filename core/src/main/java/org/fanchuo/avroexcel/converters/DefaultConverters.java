package org.fanchuo.avroexcel.converters;

import org.apache.avro.data.TimeConversions;
import org.apache.avro.generic.GenericData;

public class DefaultConverters implements IConverters {

  private final GenericData genericData;
  private final IExcelFieldParser excelFieldParser;
  private final IExcelFieldFormater excelFieldFormater;

  public DefaultConverters() {
    this.genericData = makeGenericData();
    this.excelFieldParser = makeExcelFieldParser();
    this.excelFieldFormater = makeExcelFieldFormater();
  }

  private GenericData makeGenericData() {
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

  private IExcelFieldParser makeExcelFieldParser() {
    return new ExcelFieldParser();
  }

  private IExcelFieldFormater makeExcelFieldFormater() {
    return new ExcelFieldFormater();
  }

  @Override
  public GenericData getGenericData() {
    return genericData;
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
