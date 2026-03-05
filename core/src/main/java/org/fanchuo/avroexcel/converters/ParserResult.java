package org.fanchuo.avroexcel.converters;

import org.fanchuo.avroexcel.excelutil.ErrorMessage;

public class ParserResult {
  public final ErrorMessage errorMessage;
  public final Object value;

  public ParserResult(ErrorMessage errorMessage, Object value) {
    this.errorMessage = errorMessage;
    this.value = value;
  }

  public boolean isCompatible() {
    return errorMessage == null;
  }
}
