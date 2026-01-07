package org.fanchuo.avroexcel.excelutil;

import java.util.Formatter;
import org.apache.poi.ss.util.CellAddress;

public class FormatErrorMessage implements ErrorMessage {
  private final String fmtString;
  private final CellAddress address;
  private final Object[] vargs;

  public FormatErrorMessage(String fmtString, CellAddress address, Object... vargs) {
    this.fmtString = fmtString;
    this.address = address;
    this.vargs = vargs;
  }

  @Override
  public void dump(String indent, StringBuilder builder) {
    if (this.address != null) builder.append("[").append(this.address).append("] ");
    builder.append(indent).append(new Formatter().format(this.fmtString, this.vargs));
  }
}
