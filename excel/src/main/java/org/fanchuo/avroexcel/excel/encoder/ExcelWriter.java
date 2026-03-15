package org.fanchuo.avroexcel.excel.encoder;

import java.io.*;
import java.util.*;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fanchuo.avroexcel.core.avroutil.Zone;
import org.fanchuo.avroexcel.core.encoder.GenericRecordConsumer;
import org.fanchuo.avroexcel.core.headerinfo.HeaderInfo;
import org.fanchuo.avroexcel.core.headerinfo.HeaderInfoAvroSchemaReader;
import org.fanchuo.avroexcel.core.recordgeometry.RecordGeometry;
import org.fanchuo.avroexcel.core.recordgeometry.RecordGeometryAvroReader;
import org.fanchuo.avroexcel.excel.converters.IExcelFieldFormater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelWriter implements GenericRecordConsumer {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExcelWriter.class);

  private final OutputStream outputStream;
  private final Workbook workbook = new XSSFWorkbook();
  private final Sheet sheet;
  private final IExcelFieldFormater excelFieldFormater;
  private final int col;
  private final HeaderInfo root;
  private Zone zone = Zone.ODD;
  private int idx;

  public ExcelWriter(
      Schema schema,
      OutputStream outputStream,
      String sheetName,
      IExcelFieldFormater excelFieldFormater,
      int col,
      int row) {
    this.sheet = workbook.createSheet(sheetName);
    this.outputStream = outputStream;
    this.excelFieldFormater = excelFieldFormater;
    this.col = col;
    this.idx = row;
    this.root = HeaderInfoAvroSchemaReader.visitSchema(null, schema);
    this.writeHeaders(this.col, this.idx, root, this.idx + root.rowSpan);
    this.color(this.col, this.idx, root.colSpan, root.rowSpan, Zone.HEADER);
    this.idx = this.idx + root.rowSpan;
  }

  private Row getRow(int row) {
    Row r = this.sheet.getRow(row);
    if (r == null) return this.sheet.createRow(row);
    return r;
  }

  private Cell getCell(int row, int col) {
    Row r = getRow(row);
    Cell c = r.getCell(col);
    if (c == null) return r.createCell(col);
    return c;
  }

  public void writeHeaders(int col, int row, HeaderInfo headerInfo, int maxDepth) {
    Cell c = getCell(row, col);
    c.setCellValue(headerInfo.text);
    int lastCol = col + headerInfo.colSpan - 1;
    int lastRow = row;
    if (headerInfo.subHeaders != null) {
      int offset = col;
      for (HeaderInfo subHeader : headerInfo.subHeaders) {
        writeHeaders(offset, row + (headerInfo.text == null ? 0 : 1), subHeader, maxDepth);
        offset += subHeader.colSpan;
      }
    } else {
      lastRow = maxDepth - 1;
    }
    if (headerInfo.text != null && (col < lastCol || row < lastRow)) {
      CellRangeAddress range = new CellRangeAddress(row, lastRow, col, lastCol);
      sheet.addMergedRegion(range);
    }
    this.sheet.createFreezePane(col, maxDepth);
  }

  public void color(int col, int row, int width, int height, Zone zone) {
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        this.excelFieldFormater.colorExcelField(getCell(row + j, col + i), zone);
      }
    }
  }

  public void writeRecord(
      GenericRecord record,
      HeaderInfo headerInfo,
      RecordGeometry recordGeometry,
      int col,
      int row,
      int maxDepth,
      Zone zone) {
    LOGGER.debug("record: {}", record);
    LOGGER.debug("recordGeometry: {}", recordGeometry);
    int offset = col;
    for (HeaderInfo subHeader : headerInfo.subHeaders) {
      if (record.hasField(subHeader.text)) {
        writeObject(
            record.get(subHeader.text),
            subHeader,
            recordGeometry.subRecords.get(subHeader.text),
            offset,
            row,
            maxDepth,
            zone,
            0);
      }
      offset += subHeader.colSpan;
    }
  }

  private void writeIterable(
      Iterable<?> lst,
      HeaderInfo headerInfo,
      RecordGeometry recordGeometry,
      int col,
      int row,
      Zone zone) {
    int i = 0;
    int offsetRow = row;
    for (Object o : lst) {
      RecordGeometry subList = recordGeometry.subLists.get(i++);
      int end = offsetRow + subList.rowSpan;
      writeObject(o, headerInfo, subList, col, offsetRow, end, zone, 0);
      offsetRow = end;
    }
  }

  public void writeList(
      List<?> lst,
      HeaderInfo headerInfo,
      RecordGeometry recordGeometry,
      int col,
      int row,
      int maxDepth,
      Zone zone) {
    int offset = col;
    for (HeaderInfo subHeader : headerInfo.subHeaders) {
      if ("*size".equals(subHeader.text)) {
        writeObject(
            !lst.isEmpty() ? "*" : null,
            subHeader,
            RecordGeometry.ATOM,
            offset,
            row,
            maxDepth,
            zone,
            recordGeometry.rowSpan);
      } else if ("*".equals(subHeader.text)) {
        writeIterable(lst, subHeader, recordGeometry, offset, row, zone);
      }
      offset += subHeader.colSpan;
    }
  }

  public void writeMap(
      Map<?, ?> map,
      HeaderInfo headerInfo,
      RecordGeometry recordGeometry,
      int col,
      int row,
      int maxDepth,
      Zone zone) {
    int offset = col;
    List<Object> keys = new ArrayList<>();
    List<Object> values = new ArrayList<>();
    SortedMap<?, ?> sorted = new TreeMap<>(map);
    for (Map.Entry<?, ?> entry : sorted.entrySet()) {
      keys.add(entry.getKey());
      values.add(entry.getValue());
    }
    for (HeaderInfo subHeader : headerInfo.subHeaders) {
      if ("#size".equals(subHeader.text)) {
        writeObject(
            !map.isEmpty() ? "#" : null,
            subHeader,
            RecordGeometry.ATOM,
            offset,
            row,
            maxDepth,
            zone,
            recordGeometry.rowSpan);
      } else if ("#k".equals(subHeader.text)) {
        writeIterable(keys, subHeader, recordGeometry, offset, row, zone);
      } else if ("#v".equals(subHeader.text)) {
        writeIterable(values, subHeader, recordGeometry, offset, row, zone);
      }
      offset += subHeader.colSpan;
    }
  }

  public void writeObject(
      Object value,
      HeaderInfo headerInfo,
      RecordGeometry recordGeometry,
      int col,
      int row,
      int maxDepth,
      Zone zone,
      int height) {
    if (value instanceof GenericRecord) {
      writeRecord((GenericRecord) value, headerInfo, recordGeometry, col, row, maxDepth, zone);
      return;
    }
    if (value instanceof List) {
      writeList((List<?>) value, headerInfo, recordGeometry, col, row, maxDepth, zone);
      return;
    }
    if (value instanceof Map) {
      writeMap((Map<?, ?>) value, headerInfo, recordGeometry, col, row, maxDepth, zone);
      return;
    }
    if (value == null) {
      return;
    }
    // case of a scalar value
    int offset = col;
    if (headerInfo.subHeaders != null) {
      for (HeaderInfo subHeader : headerInfo.subHeaders) {
        if (".value".equals(subHeader.text)) break;
        offset += subHeader.colSpan;
      }
    }
    Cell c = getCell(row, offset);
    excelFieldFormater.formatExcelField(c, value, zone);
    if (height > 1) {
      CellRangeAddress range = new CellRangeAddress(row, row + height - 1, offset, offset);
      this.sheet.addMergedRegion(range);
    }
  }

  public void finalize(int col, int width) {
    for (int i = col; i < col + width; i++) {
      this.sheet.autoSizeColumn(i, true);
    }
  }

  @Override
  public void close() throws IOException {
    finalize(this.col, this.root.colSpan);
    try {
      workbook.write(this.outputStream);
    } finally {
      this.outputStream.close();
    }
  }

  @Override
  public void writeRecord(GenericRecord record) {
    RecordGeometry recordGeometry = RecordGeometryAvroReader.visitRecord(record);
    this.color(this.col, this.idx, this.root.colSpan, recordGeometry.rowSpan, this.zone);
    this.writeRecord(
        record,
        this.root,
        recordGeometry,
        this.col,
        this.idx,
        this.idx + recordGeometry.rowSpan,
        this.zone);
    this.idx += recordGeometry.rowSpan;
    if (this.zone == Zone.EVEN) this.zone = Zone.ODD;
    else this.zone = Zone.EVEN;
  }
}
