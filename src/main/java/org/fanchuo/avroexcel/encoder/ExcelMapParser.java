package org.fanchuo.avroexcel.encoder;

import org.apache.avro.Schema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelMapParser {
    public static ParserResult parseMap(Map<String, ExcelRecord> records, Schema schema) {
        List<Schema> schemas = ParserTools.flatten(schema, x -> x.getType()== Schema.Type.MAP);
        for (Schema s : schemas) {
            ParserResult result = seekMatch(records, s.getValueType());
            if (result.compatible) return result;
        }
        return ParserResult.NOT_MATCH;
    }

    private static ParserResult seekMatch(Map<String, ExcelRecord> records, Schema schema) {
        Map<String, Object> payload = new HashMap<>();
        for (Map.Entry<String, ExcelRecord> entry : records.entrySet()) {
            ExcelRecord excelRecord = entry.getValue();
            if(excelRecord.candidates.containsKey(schema)) {
                Object value = excelRecord.candidates.get(schema);
                payload.put(entry.getKey(), value);
            } else return ParserResult.NOT_MATCH;
        }
        return new ParserResult(true, payload);
    }
}
