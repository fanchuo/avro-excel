package org.fanchuo.avroexcel.excel.decoder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.poi.ss.util.CellAddress;
import org.fanchuo.avroexcel.core.avroutil.CompositeErrorMessage;
import org.fanchuo.avroexcel.core.avroutil.FormatErrorMessage;
import org.fanchuo.avroexcel.core.encoder.SchemaReport;

public class ExcelRecordParser {
  private ExcelRecordParser() {
    super();
  }

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
        ExcelRecord subRecord = subRecords.remove(fieldName);
        if (subRecord.candidates.containsKey(fieldSchema)) {
          payload.put(fieldName, subRecord.candidates.get(fieldSchema));
        } else {
          CompositeErrorMessage compositeErrorMessage = new CompositeErrorMessage();
          compositeErrorMessage.add(
              new FormatErrorMessage("Failed to match record for field %s", address, fieldName));
          compositeErrorMessage.add(subRecord.failures.get(fieldSchema));
          return new ParserResult(compositeErrorMessage, null);
        }
      } else {
        CollectionTypes collectionTypes = ParserTools.collectTypes(fieldSchema);
        if (collectionTypes.nullable) payload.put(fieldName, null);
        else if (collectionTypes.listable) payload.put(fieldName, Collections.emptyList());
        else if (collectionTypes.mappable) payload.put(fieldName, Collections.emptyMap());
        else
          return new ParserResult(
              new FormatErrorMessage(
                  "Failed to find field %s for schema %s",
                  address, fieldName, new SchemaReport(recordSchema)),
              null);
      }
    }
    if (subRecords.isEmpty()) return new ParserResult(null, payload);
    return new ParserResult(
        new FormatErrorMessage(
            "Failed to match schema %s, because of additional fields defined %s",
            address, new SchemaReport(recordSchema), subRecords.keySet()),
        null);
  }
}
