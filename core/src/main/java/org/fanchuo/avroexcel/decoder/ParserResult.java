package org.fanchuo.avroexcel.decoder;

import org.fanchuo.avroexcel.excelutil.ErrorMessage;

public class ParserResult {
  final ErrorMessage errorMessage;
  final Object payload;

  ParserResult(ErrorMessage errorMessage, Object payload) {
    this.errorMessage = errorMessage;
    this.payload = payload;
  }
}
