package org.fanchuo.avroexcel.encoder;

import org.apache.avro.Schema;

import java.util.HashMap;
import java.util.Map;

public class ExcelMapParser {
    public static class MapParser {
        final boolean compatible;
        final Object payload;

        MapParser(boolean compatible, Object payload) {
            this.compatible = compatible;
            this.payload = payload;
        }
    }
    private static final MapParser NOT_MATCH = new MapParser(false, null);

    public static MapParser parseMap(Map<String, ExcelRecord> records, Schema schema) {
        Map<String, Object> payload = new HashMap<>();
        for (Map.Entry<String, ExcelRecord> entry : records.entrySet()) {
            ExcelRecord excelRecord = entry.getValue();
            if(excelRecord.candidates.containsKey(schema)) {
                Object value = excelRecord.candidates.get(schema);
                payload.put(entry.getKey(), value);
            } else return NOT_MATCH;
        }
        return new MapParser(true, payload);
    }
}
