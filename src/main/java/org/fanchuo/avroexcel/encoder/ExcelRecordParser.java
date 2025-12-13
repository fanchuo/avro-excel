package org.fanchuo.avroexcel.encoder;

import java.util.HashMap;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

public class ExcelRecordParser {
  public static ParserResult parseRecord(Map<String, ExcelRecord> subRecords, Schema schema) {
    return ParserTools.parse(
        subRecords, schema, Schema.Type.RECORD, ExcelRecordParser::parseAttempt);
  }

  private static ParserResult parseAttempt(Map<String, ExcelRecord> r, Schema recordSchema) {
    GenericRecord payload = new GenericData.Record(recordSchema);
    Map<String, ExcelRecord> subRecords = new HashMap<>(r);
    for (Schema.Field field : recordSchema.getFields()) {
      String fieldName = field.name();
      Schema fieldSchema = field.schema();
      if (subRecords.containsKey(fieldName)) {
        // 1. je trouve une valeur correspondante, le schema doit match
        ExcelRecord subRecord = subRecords.remove(fieldName);
        if (subRecord.candidates.containsKey(fieldSchema)) {
          payload.put(fieldName, subRecord.candidates.get(fieldSchema));
        } else {
          return ParserResult.NOT_MATCH;
        }
      } else {
        // 2. je ne trouve pas de valeur correspondante, le schema doit être nullable
        if (!fieldSchema.isNullable()) {
          return ParserResult.NOT_MATCH;
        }
      }
    }
    if (subRecords.isEmpty()) return new ParserResult(true, payload);
    return ParserResult.NOT_MATCH;
  }
}
