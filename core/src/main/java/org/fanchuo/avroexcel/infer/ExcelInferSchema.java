package org.fanchuo.avroexcel.infer;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.apache.avro.Schema;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.fanchuo.avroexcel.excelutil.ExcelSheetReader;
import org.fanchuo.avroexcel.headerinfo.HeaderInfo;
import org.fanchuo.avroexcel.headerinfo.HeaderInfoExcelReader;

public class ExcelInferSchema {
  private ExcelInferSchema() {}

  public static Schema inferSchema(InputStream inputStream, String sheetName, int col, int row)
      throws IOException, InferSchemaException {
    ExcelSheetReader excelSheetReader = ExcelSheetReader.loadSheet(inputStream, sheetName);
    HeaderInfo headerInfo = HeaderInfoExcelReader.visitSheet(excelSheetReader, col, row);
    row += headerInfo.rowSpan;
    DataVisitor dataVisitor = new DataVisitor();
    while (!emptyLine(excelSheetReader, col, row, headerInfo)) {
      int rowSpan = dataVisitor.visitSheet(excelSheetReader, col, row, headerInfo);
      if (rowSpan <= 0) break;
      row += rowSpan;
    }
    return makeSchema(headerInfo, dataVisitor);
  }

  private static final Type[] TYPES = Type.values();
  private static final Set<String> SPECIAL_COLS =
      new HashSet<>(Arrays.asList("*size", "#size", "#k", ".value"));

  private static Schema makeSchema(HeaderInfo headerInfo, DataVisitor dataVisitor) {
    List<Schema> union = new ArrayList<>();
    boolean[] types = dataVisitor.schemas.get(headerInfo);
    if (types != null) {
      for (int i = 0; i < TYPES.length; i++) {
        if (types[i]) {
          union.add(TYPES[i].schema);
        }
      }
    }
    List<HeaderInfo> subHeaders = headerInfo.subHeaders;
    if (subHeaders != null) {
      List<Schema.Field> fields = new ArrayList<>();
      for (HeaderInfo subHeader : subHeaders) {
        if ("*".equals(subHeader.text)) {
          union.add(Schema.createArray(makeSchema(subHeader, dataVisitor)));
        } else if ("#v".equals(subHeader.text)) {
          union.add(Schema.createMap(makeSchema(subHeader, dataVisitor)));
        } else if (!SPECIAL_COLS.contains(subHeader.text)) {
          fields.add(new Schema.Field(subHeader.text, makeSchema(subHeader, dataVisitor)));
        }
      }
      if (!fields.isEmpty()) {
        union.add(Schema.createRecord(dataVisitor.generateNext(), null, null, false, fields));
      }
    }
    if (union.size() > 1) {
      return Schema.createUnion(union);
    } else if (union.isEmpty()) {
      return Type.NULL.schema;
    }
    return union.get(0);
  }

  private static boolean emptyLine(
      ExcelSheetReader excelSheetReader, int col, int row, HeaderInfo headerInfo) {
    for (int i = col; i < col + headerInfo.colSpan; i++) {
      Cell cell = excelSheetReader.getCell(i, row);
      if (cell != null && cell.getCellType() != CellType.BLANK) {
        return false;
      }
    }
    return true;
  }
}
