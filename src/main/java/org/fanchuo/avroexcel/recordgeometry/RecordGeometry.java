package org.fanchuo.avroexcel.recordgeometry;

import java.util.List;
import java.util.Map;

public class RecordGeometry {
  public static final RecordGeometry ATOM = new RecordGeometry(1, null, null);
  public final int rowSpan;
  public final Map<String, RecordGeometry> subRecords;
  public final List<RecordGeometry> subLists;

  public RecordGeometry(
      int rowSpan, Map<String, RecordGeometry> subRecords, List<RecordGeometry> subLists) {
    this.rowSpan = rowSpan;
    this.subRecords = subRecords;
    this.subLists = subLists;
  }

  @Override
  public String toString() {
    return "RecordGeometry{"
        + "rowSpan="
        + rowSpan
        + ", subRecords="
        + subRecords
        + ", subLists="
        + subLists
        + '}';
  }
}
