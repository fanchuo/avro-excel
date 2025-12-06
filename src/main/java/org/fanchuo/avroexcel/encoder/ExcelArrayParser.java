package org.fanchuo.avroexcel.encoder;

import org.apache.avro.Schema;

import java.util.ArrayList;
import java.util.List;

public class ExcelArrayParser {
    public static ParserResult parseArray(List<ExcelRecord> records, Schema schema) {
        List<Schema> schemas = ParserTools.flatten(schema, x -> x.getType()== Schema.Type.ARRAY);
        for (Schema s : schemas) {
            ParserResult result = seekMatch(records, s.getElementType());
            if (result.compatible) return result;
        }
        return ParserResult.NOT_MATCH;
    }

    private static ParserResult seekMatch(List<ExcelRecord> records, Schema schema) {
        List<Object> payload = new ArrayList<>();
        for (ExcelRecord excelRecord : records) {
            if(excelRecord.candidates.containsKey(schema)) {
                Object value = excelRecord.candidates.get(schema);
                payload.add(value);
            } else return ParserResult.NOT_MATCH;
        }
        return new ParserResult(true, payload);
    }
}
