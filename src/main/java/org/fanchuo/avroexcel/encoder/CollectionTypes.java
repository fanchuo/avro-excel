package org.fanchuo.avroexcel.encoder;

public class CollectionTypes {
  boolean nullable;
  boolean listable;
  boolean mappable;

  @Override
  public String toString() {
    return "CollectionTypes{"
        + "nullable="
        + nullable
        + ", listable="
        + listable
        + ", mappable="
        + mappable
        + '}';
  }
}
