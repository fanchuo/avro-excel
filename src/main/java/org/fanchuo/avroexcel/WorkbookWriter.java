package org.fanchuo.avroexcel;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.apache.avro.generic.GenericRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fanchuo.avroexcel.headerinfo.HeaderInfo;
import org.fanchuo.avroexcel.recordgeometry.RecordGeometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkbookWriter implements Closeable {
  private static final Logger LOGGER = LoggerFactory.getLogger(WorkbookWriter.class);

  public enum Zone {
    HEADER,
    ODD,
    EVEN,
  }

  private final OutputStream outputStream;
  private final Workbook workbook = new XSSFWorkbook();
  private final Sheet sheet;
  private final EnumMap<Zone, CellStyle> regularStyle = new EnumMap<>(Zone.class);
  private final EnumMap<Zone, CellStyle> dateStyle = new EnumMap<>(Zone.class);
  private final EnumMap<Zone, CellStyle> datetimeStyle = new EnumMap<>(Zone.class);

  public WorkbookWriter(File excelFile, String sheetName) throws IOException {
    this(new FileOutputStream(excelFile), sheetName);
  }

  private CellStyle makeColor(IndexedColors indexedColor) {
    CellStyle style = this.workbook.createCellStyle();
    style.setVerticalAlignment(VerticalAlignment.TOP);
    style.setFillForegroundColor(indexedColor.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    return style;
  }

  public WorkbookWriter(OutputStream outputStream, String sheetName) {
    this.sheet = workbook.createSheet(sheetName);
    this.outputStream = outputStream;
    CellStyle headerStyle = this.makeColor(IndexedColors.LIGHT_YELLOW);
    CellStyle regularOddStyle = this.makeColor(IndexedColors.WHITE);
    CellStyle dateOddStyle = this.makeColor(IndexedColors.WHITE);
    CellStyle datetimeOddStyle = this.makeColor(IndexedColors.WHITE);
    CellStyle regularEvenStyle = this.makeColor(IndexedColors.GREY_25_PERCENT);
    CellStyle dateEvenStyle = this.makeColor(IndexedColors.GREY_25_PERCENT);
    CellStyle datetimeEvenStyle = this.makeColor(IndexedColors.GREY_25_PERCENT);
    dateOddStyle.setDataFormat((short) 14);
    datetimeOddStyle.setDataFormat((short) 22);
    dateEvenStyle.setDataFormat((short) 14);
    datetimeEvenStyle.setDataFormat((short) 22);
    this.regularStyle.put(Zone.HEADER, headerStyle);
    this.regularStyle.put(Zone.ODD, regularOddStyle);
    this.regularStyle.put(Zone.EVEN, regularEvenStyle);
    this.dateStyle.put(Zone.ODD, dateOddStyle);
    this.dateStyle.put(Zone.EVEN, dateEvenStyle);
    this.datetimeStyle.put(Zone.ODD, datetimeOddStyle);
    this.datetimeStyle.put(Zone.EVEN, datetimeEvenStyle);
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
        getCell(row + j, col + i).setCellStyle(this.regularStyle.get(zone));
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
            zone);
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
      writeObject(o, headerInfo, subList, col, offsetRow, end, zone);
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
        writeObject(lst.size(), subHeader, RecordGeometry.ATOM, offset, row, maxDepth, zone);
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
        writeObject(map.size(), subHeader, RecordGeometry.ATOM, offset, row, maxDepth, zone);
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
      Zone zone) {
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
      int lastRow = maxDepth - 1;
      int lastCol = col - 1 + headerInfo.colSpan;
      if (headerInfo.text != null && (col != lastCol || row != lastRow)) {
        CellRangeAddress range = new CellRangeAddress(row, lastRow, col, lastCol);
        sheet.addMergedRegion(range);
      }
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
    if (value instanceof Number) {
      c.setCellValue(((Number) value).doubleValue());
    } else if (value instanceof Boolean) {
      c.setCellValue((Boolean) value);
    } else if (value instanceof LocalDate) {
      c.setCellValue((LocalDate) value);
      c.setCellStyle(this.dateStyle.get(zone));
    } else if (value instanceof LocalDateTime) {
      c.setCellValue((LocalDateTime) value);
      c.setCellStyle(this.datetimeStyle.get(zone));
    } else {
      c.setCellValue(String.valueOf(value));
    }
    if (row + 1 != maxDepth) {
      CellRangeAddress range = new CellRangeAddress(row, maxDepth - 1, offset, offset);
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
    try {
      workbook.write(this.outputStream);
    } finally {
      this.outputStream.close();
    }
  }
}
