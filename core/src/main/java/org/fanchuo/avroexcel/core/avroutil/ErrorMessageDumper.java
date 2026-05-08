package org.fanchuo.avroexcel.core.avroutil;

public class ErrorMessageDumper {
  private ErrorMessageDumper() {}

  public static String dump(ErrorMessage errorMessage) {
    StringBuilder sb = new StringBuilder();
    errorMessage.dump("", sb);
    return sb.toString();
  }
}
