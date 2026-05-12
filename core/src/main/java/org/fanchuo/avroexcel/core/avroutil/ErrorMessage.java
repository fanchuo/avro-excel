package org.fanchuo.avroexcel.core.avroutil;

public abstract class ErrorMessage {
  public abstract void dump(String indent, StringBuilder builder);

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    this.dump("", builder);
    return builder.toString();
  }
}
