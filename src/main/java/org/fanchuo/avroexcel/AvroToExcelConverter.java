package org.fanchuo.avroexcel;

import org.apache.avro.Schema;

import java.io.File;
import java.io.IOException;

public class AvroToExcelConverter {
    private int idx;

    public void convert(File avroFile, File excelFile, String sheetName, int col, int row) throws IOException {
        try (AvroReader avroReader = new AvroReader(avroFile)) {
            Schema schema = avroReader.getSchema();
            HeaderInfo root = HeaderInfo.visitSchema(null, schema);
            try (WorkbookWriter workbookWriter = new WorkbookWriter(excelFile, sheetName)) {
                workbookWriter.writeHeaders(col, row, root, root.rowSpan);
                this.idx = root.rowSpan;
                avroReader.process(record -> {
                    RecordGeometry recordGeometry = RecordGeometry.visitRecord(record);
                    workbookWriter.writeRecord(record, root, recordGeometry, 0, idx, idx+recordGeometry.rowSpan);
                    this.idx += recordGeometry.rowSpan;
                });
                workbookWriter.finalize(col, root.colSpan);
            }
        }
    }
}