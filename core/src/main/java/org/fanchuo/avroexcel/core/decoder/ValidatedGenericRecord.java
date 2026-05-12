package org.fanchuo.avroexcel.core.decoder;

import org.apache.avro.generic.GenericRecord;
import org.fanchuo.avroexcel.core.avroutil.ErrorMessage;

public class ValidatedGenericRecord {
  private final Object address;
  private final GenericRecord genericRecord;
  private final ErrorMessage errorMessage;

  public ValidatedGenericRecord(Object address, GenericRecord genericRecord) {
    this.address = address;
    this.genericRecord = genericRecord;
    this.errorMessage = null;
  }

  public ValidatedGenericRecord(Object address, ErrorMessage errorMessage) {
    this.address = address;
    this.genericRecord = null;
    this.errorMessage = errorMessage;
  }

  public GenericRecord getGenericRecord() {
    return genericRecord;
  }

  public ErrorMessage getErrorMessage() {
    return errorMessage;
  }

  public Object getAddress() {
    return address;
  }

  public boolean isValid() {
    return this.errorMessage == null;
  }
}
