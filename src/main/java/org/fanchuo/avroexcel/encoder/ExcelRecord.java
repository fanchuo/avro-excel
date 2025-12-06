package org.fanchuo.avroexcel.encoder;

import org.apache.avro.Schema;
import org.fanchuo.avroexcel.recordgeometry.RecordGeometry;

import java.util.Map;

public class ExcelRecord {
    final Map<Schema, Object> candidates;
    final RecordGeometry recordGeometry;

    ExcelRecord(Map<Schema, Object> candidates, RecordGeometry recordGeometry) {
        this.candidates = candidates;
        this.recordGeometry = recordGeometry;
    }
}
