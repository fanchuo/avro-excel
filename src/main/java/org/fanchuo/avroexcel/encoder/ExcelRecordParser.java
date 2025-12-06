package org.fanchuo.avroexcel.encoder;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelRecordParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelRecordParser.class);

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
        LOGGER.info("parseAttempt - subRecords: {} - recordSchema: {} - payload: {}", subRecords, recordSchema, payload);
        for (Schema.Field field : recordSchema.getFields()) {
            String fieldName = field.name();
            Schema fieldSchema = field.schema();
            if (subRecords.containsKey(fieldName)) {
                // 1. je trouve une valeur correspondante, le schema doit match
                ExcelRecord subRecord = subRecords.remove(fieldName);
                if (subRecord.candidates.containsKey(fieldSchema)) {
                    payload.put(fieldName, subRecord.candidates.get(fieldSchema));
                } else {
                    LOGGER.info("Trouvé dans les membres, mais pas dans le schema: {} - {} - {}", fieldName, fieldSchema, subRecord);
                    return ParserResult.NOT_MATCH;
                }
            } else {
                // 2. je ne trouve pas de valeur correspondante, le schema doit être nullable
                if (!fieldSchema.isNullable()) {
                    LOGGER.info("Pas trouvé dans les membres, mais pas nullable");
                    return ParserResult.NOT_MATCH;
                }
            }
        }
        LOGGER.info("Champs restants: {}", subRecords);
        if (subRecords.isEmpty()) return new ParserResult(true, payload);
        return ParserResult.NOT_MATCH;
    }
}
