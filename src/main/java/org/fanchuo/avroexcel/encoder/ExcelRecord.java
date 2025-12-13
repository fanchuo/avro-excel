package org.fanchuo.avroexcel.encoder;

import java.util.Map;
import org.apache.avro.Schema;
import org.fanchuo.avroexcel.recordgeometry.RecordGeometry;

public class ExcelRecord {
  final Map<Schema, Object> candidates;
  final RecordGeometry recordGeometry;

  ExcelRecord(Map<Schema, Object> candidates, RecordGeometry recordGeometry) {
    this.candidates = candidates;
    this.recordGeometry = recordGeometry;
  }
}
