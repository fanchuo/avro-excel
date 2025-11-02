package org.fanchuo.avroexcel;

import org.apache.avro.Schema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class HeaderInfo {
    public final String text;
    public final int colSpan;
    public final int rowSpan;
    public final List<HeaderInfo> subHeaders;
    public final boolean isNull;

    private HeaderInfo(String text, List<HeaderInfo> subHeaders, int colSpan, int rowSpan, boolean isNull) {
        this.text = text;
        this.subHeaders = subHeaders;
        this.colSpan = colSpan;
        this.rowSpan = rowSpan;
        this.isNull = isNull;
    }

    public static HeaderInfo visitRecord(String name, Schema schema) {
        int colSpan = 0;
        int rowSpan = 0;
        List<HeaderInfo> subHeaders = new ArrayList<>();
        for (Schema.Field field : schema.getFields()) {
            HeaderInfo subHeader = visitSchema(field.name(), field.schema());
            subHeaders.add(subHeader);
            colSpan += subHeader.colSpan;
            rowSpan = Math.max(rowSpan, subHeader.rowSpan);
        }
        return new HeaderInfo(name, subHeaders, colSpan, rowSpan+(name==null?0:1), false);
    }

    public static HeaderInfo visitArray(String name, Schema schema) {
        List<HeaderInfo> subHeaders = new ArrayList<>();
        HeaderInfo subHeader1 = new HeaderInfo("*size", null, 1, 1, false);
        HeaderInfo subHeader2 = visitSchema("*", schema.getElementType());
        subHeaders.add(subHeader1);
        subHeaders.add(subHeader2);
        return new HeaderInfo(name, subHeaders, subHeader2.colSpan+1, subHeader2.rowSpan+1, false);
    }

    public static HeaderInfo visitMap(String name, Schema schema) {
        List<HeaderInfo> subHeaders = new ArrayList<>();
        HeaderInfo subHeader1 = new HeaderInfo("#size", null, 1, 1, false);
        HeaderInfo subHeader2 = new HeaderInfo("#k", null, 1, 1, false);
        HeaderInfo subHeader3 = visitSchema("#v", schema.getValueType());
        subHeaders.add(subHeader1);
        subHeaders.add(subHeader2);
        subHeaders.add(subHeader3);
        return new HeaderInfo(name, subHeaders, subHeader3.colSpan+2, subHeader3.rowSpan+1, false);
    }

    public static HeaderInfo visitUnion(String name, Schema schema) {
        HeaderInfo merged = null;
        for (Schema subSchema : schema.getTypes()) {
            HeaderInfo subHeaderInfo = visitSchema(name, subSchema);
            if (merged==null) merged = subHeaderInfo;
            else {
                merged = mergeHeaderInfo(merged, subHeaderInfo);
            }
        }
        return merged;
    }

    private static HeaderInfo mergeHeaderInfo(HeaderInfo hi1, HeaderInfo hi2) {
        if (hi1.isNull) return hi2;
        if (hi2.isNull) return hi1;
        if (hi1.subHeaders==null && hi2.subHeaders==null) return hi1;
        LinkedHashMap<String, HeaderInfo> l = new LinkedHashMap<>();
        int colSpan = 0;
        int rowSpan = 0;
        if (hi1.subHeaders==null) {
            l.put(".value", new HeaderInfo(".value", null, 1, 1, false));
            rowSpan = 1;
            colSpan = 1;
        } else {
            for (HeaderInfo hi : hi1.subHeaders) {
                l.put(hi.text, hi);
                rowSpan = Math.max(rowSpan, hi.rowSpan);
                colSpan+=hi.colSpan;
            }
        }
        if (hi2.subHeaders==null) {
            l.put(".value", new HeaderInfo(".value", null, 1, 1, false));
            rowSpan = Math.max(rowSpan, 1);
            colSpan+=1;
        } else {
            for (HeaderInfo hi : hi2.subHeaders) {
                HeaderInfo other = l.get(hi.text);
                HeaderInfo merged;
                if (other==null) merged = hi;
                else merged = mergeHeaderInfo(other, hi);
                l.put(hi.text, merged);
                rowSpan = Math.max(rowSpan, hi.rowSpan);
                colSpan+=hi.colSpan;
            }
        }
        return new HeaderInfo(hi1.text, new ArrayList<>(l.values()), colSpan, rowSpan+1, false);
    }

    public static HeaderInfo visitSchema(String name, Schema schema) {
        switch (schema.getType()) {
            case RECORD:
                return visitRecord(name, schema);
            case MAP:
                return visitMap(name, schema);
            case ARRAY:
                return visitArray(name, schema);
            case UNION:
                return visitUnion(name, schema);
            case NULL:
                return new HeaderInfo(name, null, 1, 1, true);
            default:
                return new HeaderInfo(name, null, 1, 1, false);
        }
    }
}
