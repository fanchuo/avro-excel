package org.fanchuo.avroexcel.core.avroutil;

import org.apache.avro.Conversions;
import org.apache.avro.data.TimeConversions;
import org.apache.avro.generic.GenericData;

public class DefaultGenericDataConf implements IGenericDataConf {
  private final GenericData genericData;

  public DefaultGenericDataConf() {
    this.genericData = makeGenericData();
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
    genericData.addLogicalTypeConversion(new Conversions.UUIDConversion());
    genericData.addLogicalTypeConversion(new Conversions.DurationConversion());
    genericData.addLogicalTypeConversion(new Conversions.DecimalConversion());
    genericData.addLogicalTypeConversion(new Conversions.BigDecimalConversion());
    return genericData;
  }

  @Override
  public GenericData getGenericData() {
    return genericData;
  }
}
