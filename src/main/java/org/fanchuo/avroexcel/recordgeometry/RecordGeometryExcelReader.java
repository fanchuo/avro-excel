package org.fanchuo.avroexcel.recordgeometry;

import org.apache.avro.Schema;
import org.fanchuo.avroexcel.excelutil.ExcelSheetReader;
import org.fanchuo.avroexcel.headerinfo.HeaderInfo;

public class RecordGeometryExcelReader {
    private RecordGeometryExcelReader() {}

    public static RecordGeometry visitSheet(ExcelSheetReader sheet, int col, int row, HeaderInfo headerInfo, Schema schema) {
        return null;
    }
}
