package org.fanchuo.avroexcel.infer;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.util.CellAddress;
import org.fanchuo.avroexcel.excelutil.ExcelSheetReader;
import org.fanchuo.avroexcel.excelutil.TimestampParser;
import org.fanchuo.avroexcel.headerinfo.CollectionDescriptor;
import org.fanchuo.avroexcel.headerinfo.HeaderInfo;

public class DataVisitor {
  private static boolean[] make(HeaderInfo hi) {
    return new boolean[Type.values().length];
  }

  final Map<HeaderInfo, boolean[]> schemas = new HashMap<>();

  private int counter = 0;

  String generateNext() {
    return "record" + (counter++);
  }

  int visitSheet(ExcelSheetReader excelSheetReader, int col, int row, HeaderInfo headerInfo)
      throws InferSchemaException {
    List<HeaderInfo> subHeaders = headerInfo.subHeaders;
    if (subHeaders == null || subHeaders.isEmpty()) {
      Type type = visitScalar(excelSheetReader, col, row);
      this.schemas.computeIfAbsent(headerInfo, DataVisitor::make)[type.ordinal()] = true;
      return 1;
    }
    int colPos = col;
    int arraySize = -1;
    CollectionDescriptor arrayCol = null;
    int mapSize = -1;
    CollectionDescriptor mapCol = null;
    int result = 1;
    for (HeaderInfo subHeader : subHeaders) {
      String colName = subHeader.text;
      if (".value".equals(colName)) {
        Type type = visitScalar(excelSheetReader, colPos, row);
        if (type != Type.NULL) {
          this.schemas.computeIfAbsent(headerInfo, DataVisitor::make)[type.ordinal()] = true;
          return 1;
        }
      } else if ("*size".equals(colName)) {
        arraySize = CollectionDescriptor.extractCollectionSize(excelSheetReader, colPos, row);
      } else if ("*".equals(colName)) {
        arrayCol = new CollectionDescriptor(colPos, subHeader);
      } else if ("#size".equals(colName)) {
        mapSize = CollectionDescriptor.extractCollectionSize(excelSheetReader, colPos, row);
      } else if ("#v".equals(colName)) {
        mapCol = new CollectionDescriptor(colPos, subHeader);
      } else if (!"#k".equals(colName)) {
        int subResult = visitSheet(excelSheetReader, colPos, row, subHeader);
        result = Math.max(result, subResult);
      }
      colPos += subHeader.colSpan;
    }
    if (arraySize > 0 && arrayCol != null) {
      int rowPos = row;
      while (arraySize > 0) {
        int subResult =
            this.visitSheet(excelSheetReader, arrayCol.col, rowPos, arrayCol.headerInfo);
        rowPos += subResult;
        arraySize -= subResult;
      }
      result = Math.max(result, rowPos - row);
    } else if (mapSize > 0 && mapCol != null) {
      int rowPos = row;
      while (mapSize > 0) {
        int subResult = this.visitSheet(excelSheetReader, mapCol.col, rowPos, mapCol.headerInfo);
        rowPos += subResult;
        mapSize -= subResult;
      }
      result = Math.max(result, rowPos - row);
    }
    return result;
  }

  private static Type visitScalar(ExcelSheetReader excelSheetReader, int col, int row)
      throws InferSchemaException {
    Cell cell = excelSheetReader.getCell(col, row);
    if (cell == null) return Type.NULL;
    switch (cell.getCellType()) {
      case BOOLEAN:
      case FORMULA:
        return Type.BOOL;
      case BLANK:
        return Type.NULL;
      case STRING:
        Instant instant = TimestampParser.parseDate(cell);
        if (instant != null) return Type.TIMESTAMP;
        return Type.STRING;
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) return Type.LOCAL_DATE;
        return Type.DOUBLE;
      default:
        CellAddress address = new CellAddress(row, col);
        throw new InferSchemaException(
            String.format(
                "Cannot encode value '%s' of type '%s' in cell '%s'",
                cell, cell.getCellStyle(), address));
    }
  }
}
