package org.example;

import org.apache.avro.Schema;

import java.io.File;
import java.io.IOException;

public class AvroToExcelConverter {
    private int idx;

    public void convert(File avroFile, File excelFile) throws IOException {
        try (org.example.AvroReader avroReader = new org.example.AvroReader(avroFile)) {
            Schema schema = avroReader.getSchema();
            org.example.HeaderInfo root = org.example.HeaderInfo.visitSchema(null, schema);
            try (org.example.WorkbookWriter workbookWriter = new org.example.WorkbookWriter(excelFile)) {
                workbookWriter.writeHeaders(0, 0, root, root.rowSpan);
                this.idx = root.rowSpan;
                avroReader.process(record -> {
                    org.example.RecordGeometry recordGeometry = org.example.RecordGeometry.visitRecord(record);
                    workbookWriter.writeRecord(record, root, recordGeometry, 0, idx, idx+recordGeometry.rowSpan);
                    this.idx += recordGeometry.rowSpan;
                });
            }
        }
    }
}