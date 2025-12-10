package org.fanchuo.avroexcel.encoder;

import org.apache.avro.Schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ExcelCollectionParser<TSource, TTargetCollection, TIterable> {
    abstract TTargetCollection empty();
    abstract Schema.Type schemaType();
    abstract Schema subSchema(Schema schema);
    abstract Iterable<TIterable> iterable(TSource source);
    abstract ExcelRecord unwrapRecord(TIterable iterable);
    abstract void aggregate(TTargetCollection collection, TIterable item, Object value);
    public  ParserResult parseCollection(TSource records, Schema schema) {
        List<Schema> schemas = ParserTools.flatten(schema, x -> x.getType()== this.schemaType());
        for (Schema s : schemas) {
            ParserResult result = seekMatch(records, this.subSchema(s));
            if (result.compatible) return result;
        }
        return ParserResult.NOT_MATCH;
    }

    private ParserResult seekMatch(TSource records, Schema schema) {
        TTargetCollection payload = this.empty();
        for (TIterable iterable : this.iterable(records)) {
            ExcelRecord excelRecord = this.unwrapRecord(iterable);
            if(excelRecord.candidates.containsKey(schema)) {
                Object value = excelRecord.candidates.get(schema);
                this.aggregate(payload, iterable, value);
            } else return ParserResult.NOT_MATCH;
        }
        return new ParserResult(true, payload);
    }

    public static final ExcelCollectionParser<List<ExcelRecord>, List<Object>, ExcelRecord> ARRAY_PARSER = new ExcelCollectionParser<>() {
        @Override
        List<Object> empty() {
            return new ArrayList<>();
        }

        @Override
        Schema.Type schemaType() {
            return Schema.Type.ARRAY;
        }

        @Override
        Schema subSchema(Schema schema) {
            return schema.getElementType();
        }

        @Override
        Iterable<ExcelRecord> iterable(List<ExcelRecord> excelRecords) {
            return excelRecords;
        }

        @Override
        ExcelRecord unwrapRecord(ExcelRecord excelRecord) {
            return excelRecord;
        }

        @Override
        void aggregate(List<Object> objects, ExcelRecord item, Object value) {
            objects.add(value);
        }
    };

    public static final ExcelCollectionParser<Map<String, ExcelRecord>, Map<String, Object>, Map.Entry<String, ExcelRecord>> MAP_PARSER = new ExcelCollectionParser<>() {
        @Override
        Map<String, Object> empty() {
            return new HashMap<>();
        }

        @Override
        Schema.Type schemaType() {
            return Schema.Type.MAP;
        }

        @Override
        Schema subSchema(Schema schema) {
            return schema.getValueType();
        }

        @Override
        Iterable<Map.Entry<String, ExcelRecord>> iterable(Map<String, ExcelRecord> stringExcelRecordMap) {
            return stringExcelRecordMap.entrySet();
        }

        @Override
        ExcelRecord unwrapRecord(Map.Entry<String, ExcelRecord> stringExcelRecordEntry) {
            return stringExcelRecordEntry.getValue();
        }

        @Override
        void aggregate(Map<String, Object> stringObjectMap, Map.Entry<String, ExcelRecord> item, Object value) {
            stringObjectMap.put(item.getKey(), value);
        }
    };
}
