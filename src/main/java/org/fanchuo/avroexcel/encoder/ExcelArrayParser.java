package org.fanchuo.avroexcel.encoder;

import org.apache.avro.Schema;

import java.util.ArrayList;
import java.util.List;

public class ExcelArrayParser {
    public static class ArrayParser {
        final boolean compatible;
        final Object payload;

        ArrayParser(boolean compatible, Object payload) {
            this.compatible = compatible;
            this.payload = payload;
        }
    }
    private static final ArrayParser NOT_MATCH = new ArrayParser(false, null);

    public static ArrayParser parseArray(List<ExcelRecord> records, Schema schema) {
        List<Object> payload = new ArrayList<>();
        for (ExcelRecord excelRecord : records) {
            if(excelRecord.candidates.containsKey(schema)) {
                Object value = excelRecord.candidates.get(schema);
                payload.add(value);
            } else return NOT_MATCH;
        }
        return new ArrayParser(true, payload);
    }
}
