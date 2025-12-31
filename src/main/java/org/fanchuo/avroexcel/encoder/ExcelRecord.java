package org.fanchuo.avroexcel.encoder;

import java.util.Map;
import org.apache.avro.Schema;
import org.fanchuo.avroexcel.recordgeometry.RecordGeometry;

public class ExcelRecord {
  final Map<Schema, Object> candidates;
  final Map<Schema, String> failures;
  final RecordGeometry recordGeometry;

  ExcelRecord(
      Map<Schema, Object> candidates, Map<Schema, String> failures, RecordGeometry recordGeometry) {
    this.candidates = candidates;
    this.failures = failures;
    this.recordGeometry = recordGeometry;
  }
}
