package org.fanchuo.avroexcel.recordgeometry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.fanchuo.avroexcel.excelutil.ExcelSheetReader;
import org.fanchuo.avroexcel.headerinfo.HeaderInfo;

public class RecordGeometryExcelReader {
  private RecordGeometryExcelReader() {}

  private static class CollectionDescriptor {
    final int col;
    final HeaderInfo headerInfo;

    CollectionDescriptor(int col, HeaderInfo headerInfo) {
      this.col = col;
      this.headerInfo = headerInfo;
    }
  }

  public static RecordGeometry visitSheet(
      ExcelSheetReader sheet, int col, int row, HeaderInfo headerInfo) {
    if (headerInfo.subHeaders == null) {
      Cell c = sheet.getCell(col, row);
      if (c != null && c.getCellType() != CellType.BLANK) return RecordGeometry.ATOM;
      return null;
    }
    Map<String, RecordGeometry> subRecords = new HashMap<>();
    int colIdx = col;
    int arraySize = -1;
    CollectionDescriptor arrayCol = null;
    int mapSize = -1;
    CollectionDescriptor mapCol = null;
    for (HeaderInfo subHeader : headerInfo.subHeaders) {
      if ("*size".equals(subHeader.text)) {
        Cell c = sheet.getCell(colIdx, row);
        if (c != null && c.getCellType() == CellType.NUMERIC) {
          arraySize = (int) Math.round(c.getNumericCellValue());
        }
      } else if ("*".equals(subHeader.text)) {
        arrayCol = new CollectionDescriptor(colIdx, subHeader);
      } else if ("#size".equals(subHeader.text)) {
        Cell c = sheet.getCell(colIdx, row);
        if (c != null && c.getCellType() == CellType.NUMERIC) {
          mapSize = (int) Math.round(c.getNumericCellValue());
        }
      } else if ("#v".equals(subHeader.text)) {
        mapCol = new CollectionDescriptor(colIdx, subHeader);
      } else if (".value".equals(subHeader.text)) {
        Cell c = sheet.getCell(colIdx, row);
        if (c != null && c.getCellType() != CellType.BLANK) return RecordGeometry.ATOM;
      } else {
        RecordGeometry field = visitSheet(sheet, colIdx, row, subHeader);
        if (field != null) subRecords.put(subHeader.text, field);
      }
      colIdx += subHeader.colSpan;
    }

    if (arraySize != -1 && arrayCol != null) {
      return visitCollection(sheet, row, arraySize, arrayCol);
    }
    if (mapSize != -1 && mapCol != null) {
      return visitCollection(sheet, row, mapSize, mapCol);
    }
    if (subRecords.isEmpty()) return null;
    int rowSpan = 0;
    for (RecordGeometry field : subRecords.values()) rowSpan = Math.max(rowSpan, field.rowSpan);
    return new RecordGeometry(rowSpan, subRecords, null);
  }

  private static RecordGeometry visitCollection(
      ExcelSheetReader sheet,
      int row,
      int collectionSize,
      CollectionDescriptor collectionDescriptor) {
    List<RecordGeometry> subList = new ArrayList<>();
    int rowIdx = row;
    int rowSpan = 0;
    for (int i = 0; i < collectionSize; i++) {
      RecordGeometry entry =
          visitSheet(sheet, collectionDescriptor.col, rowIdx, collectionDescriptor.headerInfo);
      if (entry == null) entry = RecordGeometry.ATOM;
      subList.add(entry);
      rowIdx += entry.rowSpan;
      rowSpan += entry.rowSpan;
    }
    return new RecordGeometry(rowSpan, null, subList);
  }
}
