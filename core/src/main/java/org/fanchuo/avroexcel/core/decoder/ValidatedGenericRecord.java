package org.fanchuo.avroexcel.core.decoder;

import org.apache.avro.generic.GenericRecord;
import org.fanchuo.avroexcel.core.avroutil.ErrorMessage;

public class ValidatedGenericRecord {
  private final GenericRecord genericRecord;
  private final ErrorMessage errorMessage;

  public ValidatedGenericRecord(GenericRecord genericRecord) {
    this.genericRecord = genericRecord;
    this.errorMessage = null;
  }

  public ValidatedGenericRecord(ErrorMessage errorMessage) {
    this.genericRecord = null;
    this.errorMessage = errorMessage;
  }

  public GenericRecord getGenericRecord() {
    return genericRecord;
  }

  public ErrorMessage getErrorMessage() {
    return errorMessage;
  }

  public boolean isValid() {
    return this.errorMessage == null;
  }
}
