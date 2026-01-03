package org.fanchuo.avroexcel.excelutil;

import java.util.Formatter;

public class FormatErrorMessage implements ErrorMessage {
  private final String fmtString;
  private final Object[] vargs;

  public FormatErrorMessage(String fmtString, Object... vargs) {
    this.fmtString = fmtString;
    this.vargs = vargs;
  }

  @Override
  public void dump(String indent, StringBuilder builder) {
    builder.append(indent).append(new Formatter().format(this.fmtString, this.vargs));
  }
}
