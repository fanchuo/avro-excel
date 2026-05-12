package org.fanchuo.avroexcel.core.avroutil;

import java.util.Formatter;

public class FormatErrorMessage extends ErrorMessage {
  private final String fmtString;
  private final Object address;
  private final Object[] vargs;

  public FormatErrorMessage(String fmtString, Object address, Object... vargs) {
    this.fmtString = fmtString;
    this.address = address;
    this.vargs = vargs;
  }

  @Override
  public void dump(String indent, StringBuilder builder) {
    builder.append(indent);
    builder.append("[").append(this.address).append("] ");
    builder.append(new Formatter().format(this.fmtString, this.vargs));
  }
}
