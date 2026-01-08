package org.fanchuo.avroexcel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.avro.Schema;
import org.fanchuo.avroexcel.headerinfo.HeaderInfo;
import org.fanchuo.avroexcel.headerinfo.HeaderInfoAvroSchemaReader;
import org.fanchuo.avroexcel.recordgeometry.RecordGeometry;
import org.fanchuo.avroexcel.recordgeometry.RecordGeometryAvroReader;

public class AvroToExcelConverter {
  private int idx;
  private WorkbookWriter.Zone zone;

  public void convert(File avroFile, File excelFile, String sheetName, int col, int row)
      throws IOException {
    try (AvroReader avroReader = new AvroReader(avroFile)) {
      try (WorkbookWriter workbookWriter = new WorkbookWriter(excelFile, sheetName)) {
        convert(col, row, avroReader, workbookWriter);
      }
    }
  }

  public void convert(
      InputStream avroStream, OutputStream excelStream, String sheetName, int col, int row)
      throws IOException {
    try (AvroReader avroReader = new AvroReader(avroStream)) {
      try (WorkbookWriter workbookWriter = new WorkbookWriter(excelStream, sheetName)) {
        convert(col, row, avroReader, workbookWriter);
      }
    }
  }

  private void convert(int col, int row, AvroReader avroReader, WorkbookWriter workbookWriter) {
    this.idx = 0;
    this.zone = WorkbookWriter.Zone.ODD;
    Schema schema = avroReader.getSchema();
    HeaderInfo root = HeaderInfoAvroSchemaReader.visitSchema(null, schema);
    this.idx = row + root.rowSpan;
    workbookWriter.writeHeaders(col, row, root, this.idx);
    workbookWriter.color(col, row, root.colSpan, root.rowSpan, WorkbookWriter.Zone.HEADER);
    avroReader.process(
        record -> {
          RecordGeometry recordGeometry = RecordGeometryAvroReader.visitRecord(record);
          workbookWriter.color(col, this.idx, root.colSpan, recordGeometry.rowSpan, zone);
          workbookWriter.writeRecord(
              record, root, recordGeometry, col, idx, idx + recordGeometry.rowSpan, zone);
          this.idx += recordGeometry.rowSpan;
          if (this.zone == WorkbookWriter.Zone.EVEN) this.zone = WorkbookWriter.Zone.ODD;
          else this.zone = WorkbookWriter.Zone.EVEN;
        });
    workbookWriter.finalize(col, root.colSpan);
  }
}
