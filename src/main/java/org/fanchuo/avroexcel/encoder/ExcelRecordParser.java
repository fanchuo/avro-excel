package org.fanchuo.avroexcel.encoder;

import java.util.ArrayList;
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
          String errorMessage = subRecord.failures.get(fieldSchema);
          return new ParserResult(
              String.format("Failed to match schema %s, because %s", fieldSchema, errorMessage),
              null);
        }
      } else {
        // 2. je ne trouve pas de valeur correspondante, le schema doit être nullable
        CollectionTypes collectionTypes = ParserTools.collectTypes(fieldSchema);
        if (collectionTypes.nullable) {
          payload.put(fieldName, null);
        } else if (collectionTypes.listable) {
          payload.put(fieldName, new ArrayList<>());
        } else if (collectionTypes.mappable) {
          payload.put(fieldName, new HashMap<>());
        } else {
          return new ParserResult(
              String.format("Failed to match schema %s, because not nullable", fieldSchema), null);
        }
      }
    }
    if (subRecords.isEmpty()) return new ParserResult(null, payload);
    return new ParserResult(
        String.format(
            "Failed to match schema %s, because of additional fields defined %s",
            recordSchema, subRecords.keySet()),
        null);
  }
}
