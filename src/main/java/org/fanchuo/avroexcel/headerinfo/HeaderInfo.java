package org.fanchuo.avroexcel.headerinfo;

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

    HeaderInfo(String text, List<HeaderInfo> subHeaders, int colSpan, int rowSpan, boolean isNull) {
        this.text = text;
        this.subHeaders = subHeaders;
        this.colSpan = colSpan;
        this.rowSpan = rowSpan;
        this.isNull = isNull;
    }

}
