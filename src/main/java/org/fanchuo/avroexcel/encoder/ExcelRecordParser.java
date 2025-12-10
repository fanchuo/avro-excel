package org.fanchuo.avroexcel.encoder;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelRecordParser {
    public static ParserResult parseRecord(Map<String, ExcelRecord> subRecords, Schema schema) {
        List<Schema> schemas = ParserTools.flatten(schema, x->x.getType()== Schema.Type.RECORD);
        for (Schema s : schemas) {
            GenericRecord payload = new GenericData.Record(s);
            ParserResult parseAttempt = parseAttempt(new HashMap<>(subRecords), s, payload);
            if (parseAttempt.compatible) return parseAttempt;
        }
        return ParserResult.NOT_MATCH;
    }

    private static ParserResult parseAttempt(Map<String, ExcelRecord> subRecords, Schema recordSchema, GenericRecord payload) {
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
