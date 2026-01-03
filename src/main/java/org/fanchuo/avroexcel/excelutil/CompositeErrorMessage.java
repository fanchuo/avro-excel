package org.fanchuo.avroexcel.excelutil;

import java.util.ArrayList;
import java.util.List;

public class CompositeErrorMessage implements ErrorMessage {
  private final List<ErrorMessage> subMessages = new ArrayList<>();

  public void add(ErrorMessage errorMessage) {
    this.subMessages.add(errorMessage);
  }

  public int size() {
    return this.subMessages.size();
  }

  @Override
  public void dump(String indent, StringBuilder builder) {
    builder.append(indent).append("Caused by:\n");
    String subIndent = indent + "  ";
    for (ErrorMessage msg : this.subMessages) {
      msg.dump(subIndent, builder);
      builder.append('\n');
    }
  }
}
