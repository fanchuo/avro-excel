package org.fanchuo.avroexcel.headerinfo;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellRangeAddress;
import org.fanchuo.avroexcel.excelutil.ExcelSheetReader;

public class CollectionDescriptor {
  public final int col;
  public final HeaderInfo headerInfo;

  public CollectionDescriptor(int col, HeaderInfo headerInfo) {
    this.col = col;
    this.headerInfo = headerInfo;
  }

  public static int extractCollectionSize(ExcelSheetReader sheet, int col, int row) {
    Cell c = sheet.getCell(col, row);
    if (c != null && c.getCellType() != CellType.BLANK) {
      CellRangeAddress cellRangeAddress = sheet.getRangeAt(col, row);
      if (cellRangeAddress == null) {
        return 1;
      }
      return cellRangeAddress.getLastRow() - cellRangeAddress.getFirstRow() + 1;
    }
    return -1;
  }
}
