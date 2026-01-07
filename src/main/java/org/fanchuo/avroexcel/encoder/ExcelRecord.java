package org.fanchuo.avroexcel.encoder;

import java.util.Map;
import org.apache.avro.Schema;
import org.fanchuo.avroexcel.excelutil.ErrorMessage;
import org.fanchuo.avroexcel.recordgeometry.RecordGeometry;

public class ExcelRecord {
  final Map<Schema, Object> candidates;
  final Map<Schema, ErrorMessage> failures;
  final RecordGeometry recordGeometry;
  final boolean empty;

  ExcelRecord(
      Map<Schema, Object> candidates,
      Map<Schema, ErrorMessage> failures,
      RecordGeometry recordGeometry,
      boolean empty) {
    this.candidates = candidates;
    this.failures = failures;
    this.recordGeometry = recordGeometry;
    this.empty = empty;
  }
}
