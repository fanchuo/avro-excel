package org.fanchuo.avroexcel;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import org.apache.avro.Schema;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.fanchuo.avroexcel.excelutil.ExcelSheetReader;
import org.fanchuo.avroexcel.excelutil.TimestampParser;
import org.fanchuo.avroexcel.headerinfo.CollectionDescriptor;
import org.fanchuo.avroexcel.headerinfo.HeaderInfo;
import org.fanchuo.avroexcel.headerinfo.HeaderInfoExcelReader;

public class ExcelInferSchema {
  private static final Schema DATETIME =
      new Schema.Parser().parse("{\"type\": \"long\", \"logicalType\": \"timestamp-millis\"}");
  private static final Schema LOCAL_DATETIME =
      new Schema.Parser()
          .parse("{\"type\": \"long\", \"logicalType\": \"local-timestamp-millis\"}");

  private enum Type {
    STRING(Schema.create(Schema.Type.STRING)),
    NULL(Schema.create(Schema.Type.NULL)),
    DOUBLE(Schema.create(Schema.Type.DOUBLE)),
    LOCAL_DATE(LOCAL_DATETIME),
    TIMESTAMP(DATETIME),
    BOOL(Schema.create(Schema.Type.BOOLEAN));

    final Schema schema;

    Type(Schema schema) {
      this.schema = schema;
    }
  }

  private static final Type[] TYPES = Type.values();

  private static class DataVisitor {
    private static boolean[] make(HeaderInfo hi) {
      return new boolean[TYPES.length];
    }

    final Map<HeaderInfo, boolean[]> schemas = new HashMap<>();

    private int counter = 0;

    String generateNext() {
      return "record" + (counter++);
    }

    int visitSheet(ExcelSheetReader excelSheetReader, int col, int row, HeaderInfo headerInfo) {
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

    private static Type visitScalar(ExcelSheetReader excelSheetReader, int col, int row) {
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
          throw new RuntimeException("Il a prou-prou-prout-prouté");
      }
    }
  }

  public static Schema inferSchema(InputStream inputStream, String sheetName, int col, int row)
      throws IOException {
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
      return null;
    }
    return union.getFirst();
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
