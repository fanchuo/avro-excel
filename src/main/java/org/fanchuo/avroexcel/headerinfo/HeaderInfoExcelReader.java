package org.fanchuo.avroexcel.headerinfo;

import java.util.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellRangeAddress;
import org.fanchuo.avroexcel.excelutil.ExcelSheetReader;

public class HeaderInfoExcelReader {
  private HeaderInfoExcelReader() {}

  public static HeaderInfo visitSheet(ExcelSheetReader sheet, int col, int row) {
    List<HeaderInfo> subHeaders = new ArrayList<>();
    int x = col;
    int colSpan = 0;
    int rowSpan = 0;
    while (true) {
      HeaderInfo subHeader = visitSub(sheet, x, row);
      if (subHeader == null) break;
      x += subHeader.colSpan;
      subHeaders.add(subHeader);
      colSpan += subHeader.colSpan;
      rowSpan = Math.max(rowSpan, subHeader.rowSpan);
    }
    return new HeaderInfo(null, subHeaders, colSpan, rowSpan, false);
  }

  private static HeaderInfo visitSub(ExcelSheetReader sheet, int col, int row) {
    Cell cell = sheet.getCell(col, row);
    if (cell == null) return null;
    if (cell.getCellType() == CellType.BLANK) return null;
    CellRangeAddress range = sheet.getRangeAt(col, row);
    if (range != null && (range.getFirstColumn() != range.getLastColumn())) {
      List<HeaderInfo> subHeaders = new ArrayList<>();
      int colSpan = range.getLastColumn() - range.getFirstColumn() + 1;
      int rowSpan = 0;
      int x = col;
      int end = x + colSpan;
      while (x < end) {
        HeaderInfo subHeader = visitSub(sheet, x, row + 1);
        if (subHeader == null) break;
        subHeaders.add(subHeader);
        x += subHeader.colSpan;
        rowSpan = Math.max(rowSpan, subHeader.rowSpan);
      }
      return new HeaderInfo(cell.getStringCellValue(), subHeaders, colSpan, rowSpan + 1, false);
    }
    return new HeaderInfo(cell.getStringCellValue(), null, 1, 1, false);
  }
}
