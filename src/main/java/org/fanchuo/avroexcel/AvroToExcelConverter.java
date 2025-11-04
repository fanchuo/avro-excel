package org.fanchuo.avroexcel;

import org.apache.avro.Schema;
import org.fanchuo.avroexcel.headerinfo.HeaderInfo;
import org.fanchuo.avroexcel.headerinfo.HeaderInfoAvroSchemaReader;

import java.io.File;
import java.io.IOException;

public class AvroToExcelConverter {
    private int idx;
    private WorkbookWriter.Zone zone;

    public void convert(File avroFile, File excelFile, String sheetName, int col, int row) throws IOException {
        this.idx = 0;
        this.zone = WorkbookWriter.Zone.ODD;
        try (AvroReader avroReader = new AvroReader(avroFile)) {
            Schema schema = avroReader.getSchema();
            HeaderInfo root = HeaderInfoAvroSchemaReader.visitSchema(null, schema);
            try (WorkbookWriter workbookWriter = new WorkbookWriter(excelFile, sheetName)) {
                this.idx = row + root.rowSpan;
                workbookWriter.writeHeaders(col, row, root, this.idx);
                workbookWriter.color(col, row, root.colSpan, root.rowSpan, WorkbookWriter.Zone.HEADER);
                avroReader.process(record -> {
                    RecordGeometry recordGeometry = RecordGeometry.visitRecord(record);
                    workbookWriter.color(col, this.idx, root.colSpan, recordGeometry.rowSpan, zone);
                    workbookWriter.writeRecord(record, root, recordGeometry, col, idx, idx+recordGeometry.rowSpan, zone);
                    this.idx += recordGeometry.rowSpan;
                    if (this.zone == WorkbookWriter.Zone.EVEN) this.zone = WorkbookWriter.Zone.ODD;
                    else this.zone = WorkbookWriter.Zone.EVEN;
                });
                workbookWriter.finalize(col, root.colSpan);
            }
        }
    }
}