package org.fanchuo.avroexcel.encoder;

import java.util.HashMap;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.poi.ss.util.CellAddress;
import org.fanchuo.avroexcel.excelutil.CompositeErrorMessage;
import org.fanchuo.avroexcel.excelutil.FormatErrorMessage;

public class ExcelRecordParser {
  public static ParserResult parseRecord(
      Map<String, ExcelRecord> subRecords, Schema schema, CellAddress address) {
    return ParserTools.parse(
        subRecords, schema, Schema.Type.RECORD, ExcelRecordParser::parseAttempt, address);
  }

  private static ParserResult parseAttempt(
      Map<String, ExcelRecord> r, Schema recordSchema, CellAddress address) {
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
          CompositeErrorMessage compositeErrorMessage = new CompositeErrorMessage();
          compositeErrorMessage.add(
              new FormatErrorMessage("Failed to match schema %s", address, fieldSchema));
          compositeErrorMessage.add(subRecord.failures.get(fieldSchema));
          return new ParserResult(compositeErrorMessage, null);
        }
      } else {
        // 2. je ne trouve pas de valeur correspondante, le schema doit être nullable
        return new ParserResult(
                new FormatErrorMessage(
                        "Failed to find field %s for schema %s", address, fieldName, recordSchema),
                null);
      }
    }
    if (subRecords.isEmpty()) return new ParserResult(null, payload);
    return new ParserResult(
        new FormatErrorMessage(
            "Failed to match schema %s, because of additional fields defined %s",
            address, recordSchema, subRecords.keySet()),
        null);
  }
}
