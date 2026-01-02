package org.fanchuo.avroexcel.encoder;

public class ParserResult {
  final String errorMessage;
  final Object payload;

  ParserResult(String errorMessage, Object payload) {
    this.errorMessage = errorMessage;
    this.payload = payload;
  }
}
