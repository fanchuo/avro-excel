package org.fanchuo.avroexcel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.fanchuo.avroexcel.headerinfo.HeaderInfo;
import org.fanchuo.avroexcel.headerinfo.HeaderInfoAvroSchemaReader;
import org.fanchuo.avroexcel.recordgeometry.RecordGeometry;
import org.fanchuo.avroexcel.recordgeometry.RecordGeometryAvroReader;

public class AvroToExcelConverter {
  public static void convert(File avroFile, File excelFile, String sheetName, int col, int row)
      throws IOException {
    try (AvroReader avroReader = new AvroReader(avroFile);
        WorkbookWriter workbookWriter = new WorkbookWriter(excelFile, sheetName)) {
      convert(col, row, avroReader, workbookWriter);
    }
  }

  public static void convert(
      InputStream avroStream, OutputStream excelStream, String sheetName, int col, int row)
      throws IOException {
    try (AvroReader avroReader = new AvroReader(avroStream);
        WorkbookWriter workbookWriter = new WorkbookWriter(excelStream, sheetName)) {
      convert(col, row, avroReader, workbookWriter);
    }
  }

  private static void convert(
      int col, int row, AvroReader avroReader, WorkbookWriter workbookWriter) {
    Schema schema = avroReader.getSchema();
    HeaderInfo root = HeaderInfoAvroSchemaReader.visitSchema(null, schema);
    workbookWriter.writeHeaders(col, row, root, row + root.rowSpan);
    workbookWriter.color(col, row, root.colSpan, root.rowSpan, WorkbookWriter.Zone.HEADER);
    avroReader.process(
        new Consumer<>() {
          WorkbookWriter.Zone zone = WorkbookWriter.Zone.ODD;
          int idx = row + root.rowSpan;

          @Override
          public void accept(GenericRecord record) {
            RecordGeometry recordGeometry = RecordGeometryAvroReader.visitRecord(record);
            workbookWriter.color(col, idx, root.colSpan, recordGeometry.rowSpan, zone);
            workbookWriter.writeRecord(
                record, root, recordGeometry, col, idx, idx + recordGeometry.rowSpan, zone);
            idx += recordGeometry.rowSpan;
            if (zone == WorkbookWriter.Zone.EVEN) zone = WorkbookWriter.Zone.ODD;
            else zone = WorkbookWriter.Zone.EVEN;
          }
        });
    workbookWriter.finalize(col, root.colSpan);
  }
}
