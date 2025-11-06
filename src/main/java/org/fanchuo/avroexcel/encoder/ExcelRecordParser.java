package org.fanchuo.avroexcel.encoder;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelRecordParser {
    public static class RecordParser {
        final boolean compatible;
        final GenericRecord payload;

        RecordParser(boolean compatible, GenericRecord payload) {
            this.compatible = compatible;
            this.payload = payload;
        }
    }

    private static final RecordParser NO_MATCH = new RecordParser(false, null);
    public static RecordParser parseRecord(Map<String, ExcelRecord> subRecords, Schema schema) {
        List<Schema> schemas = ParserTools.flatten(schema, x->x.getType()== Schema.Type.RECORD);
        for (Schema s : schemas) {
            GenericRecord payload = new GenericData.Record(schema);
            RecordParser parseAttempt = parseAttempt(new HashMap<>(subRecords), s, payload);
            if (parseAttempt.compatible) return parseAttempt;
        }
        return NO_MATCH;
    }

    private static RecordParser parseAttempt(Map<String, ExcelRecord> subRecords, Schema recordSchema, GenericRecord payload) {
        for (Schema.Field field : recordSchema.getFields()) {
            String fieldName = field.name();
            Schema fieldSchema = field.schema();
            if (fieldSchema.getType() == Schema.Type.NULL) {
                if (subRecords.containsKey(fieldName)) return NO_MATCH;
            } else {
                if (!subRecords.containsKey(fieldName)) return NO_MATCH;
                ExcelRecord subRecord = subRecords.get(fieldName);
                if (subRecord.candidates.containsKey(fieldSchema)) {
                    payload.put(fieldName, subRecord.candidates.remove(fieldSchema));
                } else {
                    return NO_MATCH;
                }
            }
        }
        if (subRecords.isEmpty()) return new RecordParser(true, payload);
        return NO_MATCH;
    }
}
