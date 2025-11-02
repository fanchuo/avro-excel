package org.example;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordGeometry {
    public static final RecordGeometry ATOM = new RecordGeometry(1, null, null);
    public final int rowSpan;
    public final Map<String, RecordGeometry> subRecords;
    public final List<RecordGeometry> subLists;

    private RecordGeometry(int rowSpan, Map<String, RecordGeometry> subRecords, List<RecordGeometry> subLists) {
        this.rowSpan = rowSpan;
        this.subRecords = subRecords;
        this.subLists = subLists;
    }

    public static RecordGeometry visitObject(Object value) {
        if (value instanceof GenericRecord) {
            return visitRecord((GenericRecord) value);
        }
        if (value instanceof List) {
            return visitList((List<?>) value);
        }
        if (value instanceof Map) {
            return visitMap((Map<?,?>) value);
        }
        return ATOM;
    }

    public static RecordGeometry visitRecord(GenericRecord record) {
        Schema schema = record.getSchema();
        Map<String, RecordGeometry> subRecords = new HashMap<>();
        int maxSoFar = 0;
        for (Schema.Field field : schema.getFields()) {
            Object value = record.get(field.name());
            RecordGeometry subRecord = visitObject(value);
            subRecords.put(field.name(), subRecord);
            maxSoFar = Math.max(maxSoFar, subRecord.rowSpan);
        }
        return new RecordGeometry(maxSoFar, subRecords, null);
    }

    public static RecordGeometry visitList(List<?> list) {
        List<RecordGeometry> subLists = new ArrayList<>();
        int cumul = 0;
        for (Object value : list) {
            RecordGeometry subList = visitObject(value);
            cumul += subList.rowSpan;
            subLists.add(subList);
        }
        if (cumul==0) return ATOM;
        return new RecordGeometry(cumul, null, subLists);
    }

    public static RecordGeometry visitMap(Map<?, ?> map) {
        List<RecordGeometry> subLists = new ArrayList<>();
        int cumul = 0;
        for (Object value : map.values()) {
            RecordGeometry subList = visitObject(value);
            cumul += subList.rowSpan;
            subLists.add(subList);
        }
        if (cumul==0) return ATOM;
        return new RecordGeometry(cumul, null, subLists);
    }

    @Override
    public String toString() {
        return "RecordGeometry{" +
                "rowSpan=" + rowSpan +
                ", subRecords=" + subRecords +
                ", subLists=" + subLists +
                '}';
    }
}
