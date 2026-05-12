package org.fanchuo.avroexcel.core.avroutil;

@FunctionalInterface
public interface ErrorHandler {
  void handle(ErrorMessage errorMessage);
}
