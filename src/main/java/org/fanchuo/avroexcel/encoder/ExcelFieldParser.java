package org.fanchuo.avroexcel.encoder;

import java.text.ParsePosition;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import org.apache.avro.Schema;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.fanchuo.avroexcel.excelutil.ErrorMessage;
import org.fanchuo.avroexcel.excelutil.FormatErrorMessage;

public class ExcelFieldParser {
  public abstract static class TypeParser {
    public ErrorMessage errorMessage;
    public Object value;

    public abstract void analyze(Schema schema, Cell cell);

    public boolean isCompatible() {
      return errorMessage == null;
    }
  }

  static class EnumExcelFieldParser extends TypeParser {
    @Override
    public void analyze(Schema schema, Cell cell) {
      if (cell.getCellType() == CellType.STRING) {
        String str = cell.getStringCellValue();
        if (schema.getEnumSymbols().contains(str)) {
          this.errorMessage = null;
          this.value = str;
        } else {
          this.errorMessage =
              new FormatErrorMessage("'%s' is not one of %s", str, schema.getEnumSymbols());
        }
      } else {
        this.errorMessage =
            new FormatErrorMessage("Cell type '%s' is not STRING", cell.getCellType());
      }
    }
  }

  static class StringExcelFieldParser extends TypeParser {
    @Override
    public void analyze(Schema schema, Cell cell) {
      if (cell.getCellType() == CellType.STRING) {
        this.errorMessage = null;
        this.value = cell.getStringCellValue();
      } else {
        this.errorMessage =
            new FormatErrorMessage("Cell type '%s' is not STRING", cell.getCellType());
      }
    }
  }

  private static final Set<String> LOCALDATE_LOGICAL_TYPES =
      new CopyOnWriteArraySet<>(
          Arrays.asList(
              "date",
              "time-millis",
              "time-micros",
              "local-timestamp-millis",
              "local-timestamp-micros",
              "local-timestamp-nanos"));

  private static final Set<String> TIMESTAMP_LOGICAL_TYPES =
      new CopyOnWriteArraySet<>(
          Arrays.asList("timestamp-millis", "timestamp-micros", "timestamp-nanos"));

  /**
   * @param cell An Excel cell
   * @return If it is formatted as a date
   * @see org.apache.poi.ss.usermodel.BuiltinFormats
   */
  private static boolean isDate(Cell cell) {
    CellStyle style = cell.getCellStyle();
    if (style == null) return false;
    short dataFormat = style.getDataFormat();
    return dataFormat >= 0xe && dataFormat <= 0x16;
  }

  abstract static class AbstractIntExcelFieldParser<T extends Number> extends TypeParser {
    abstract T getIntValue(double v);

    @Override
    public void analyze(Schema schema, Cell cell) {
      String logicalType =
          schema.getLogicalType() == null ? null : schema.getLogicalType().getName();
      if (logicalType != null && LOCALDATE_LOGICAL_TYPES.contains(logicalType)) {
        if (cell.getCellType() == CellType.NUMERIC && isDate(cell)) {
          this.errorMessage = null;
          if ("date".equals(logicalType))
            this.value = cell.getLocalDateTimeCellValue().toLocalDate();
          else if (logicalType.startsWith("time-"))
            this.value = cell.getLocalDateTimeCellValue().toLocalTime();
          else this.value = cell.getLocalDateTimeCellValue();
        } else {
          this.errorMessage =
              new FormatErrorMessage(
                  "Not a date cell type (type: %s, format: %s)",
                  cell.getCellType(), cell.getCellStyle().getDataFormat());
        }
      } else if (TIMESTAMP_LOGICAL_TYPES.contains(logicalType)) {
        if (cell.getCellType() == CellType.STRING) {
          String str = cell.getStringCellValue();
          ParsePosition position = new ParsePosition(0);
          TemporalAccessor temporalAccessor =
              DateTimeFormatter.ISO_INSTANT.parseUnresolved(str, position);
          if (position.getErrorIndex() < 0) {
            this.errorMessage = null;
            this.value = Instant.from(temporalAccessor);
          } else {
            this.errorMessage =
                new FormatErrorMessage("Cell format '%s' is not ISO8601 format", str);
          }
        } else {
          this.errorMessage =
              new FormatErrorMessage("Cell type '%s' is not STRING", cell.getCellType());
        }
      } else if (cell.getCellType() == CellType.NUMERIC) {
        this.errorMessage = null;
        this.value = getIntValue(cell.getNumericCellValue());
      } else {
        this.errorMessage =
            new FormatErrorMessage("Cell type '%s' is not NUMERIC", cell.getCellType());
      }
    }
  }

  static class IntExcelFieldParser extends AbstractIntExcelFieldParser<Integer> {
    @Override
    Integer getIntValue(double v) {
      return (int) v;
    }
  }

  static class LongExcelFieldParser extends AbstractIntExcelFieldParser<Long> {
    @Override
    Long getIntValue(double v) {
      return (long) v;
    }
  }

  abstract static class AbstractFloatExcelFieldParser<T extends Number> extends TypeParser {
    abstract T getFloatValue(double v);

    @Override
    public void analyze(Schema schema, Cell cell) {
      if (cell.getCellType() == CellType.NUMERIC) {
        this.errorMessage = null;
        this.value = getFloatValue(cell.getNumericCellValue());
      } else {
        this.errorMessage =
            new FormatErrorMessage("Cell type '%s' is not NUMERIC", cell.getCellType());
      }
    }
  }

  static class FloatExcelFieldParser extends AbstractFloatExcelFieldParser<Float> {
    @Override
    Float getFloatValue(double v) {
      return (float) v;
    }
  }

  static class DoubleExcelFieldParser extends AbstractFloatExcelFieldParser<Double> {
    @Override
    Double getFloatValue(double v) {
      return v;
    }
  }

  static class BooleanExcelFieldParser extends TypeParser {
    @Override
    public void analyze(Schema schema, Cell cell) {
      if (cell.getCellType() == CellType.BOOLEAN) {
        this.errorMessage = null;
        this.value = cell.getBooleanCellValue();
      } else {
        this.errorMessage =
            new FormatErrorMessage("Cell type '%s' is not NUMERIC", cell.getCellType());
      }
    }
  }

  static class NullExcelFieldParser extends TypeParser {
    @Override
    public void analyze(Schema schema, Cell cell) {}
  }

  private final NullExcelFieldParser NULL_PARSER = new NullExcelFieldParser();
  private final EnumMap<Schema.Type, TypeParser> registry = new EnumMap<>(Schema.Type.class);

  public ExcelFieldParser() {
    registry.put(Schema.Type.ENUM, new EnumExcelFieldParser());
    registry.put(Schema.Type.STRING, new StringExcelFieldParser());
    registry.put(Schema.Type.INT, new IntExcelFieldParser());
    registry.put(Schema.Type.LONG, new LongExcelFieldParser());
    registry.put(Schema.Type.FLOAT, new FloatExcelFieldParser());
    registry.put(Schema.Type.DOUBLE, new DoubleExcelFieldParser());
    registry.put(Schema.Type.BOOLEAN, new BooleanExcelFieldParser());
  }

  public TypeParser checkCompatible(Schema s, Cell cell) {
    if (cell == null || cell.getCellType() == CellType.BLANK) {
      NULL_PARSER.errorMessage =
          s.isNullable() ? null : new FormatErrorMessage("Cell is not BLANK");
      return NULL_PARSER;
    }
    List<Schema> schemas = ParserTools.flatten(s, x -> registry.containsKey(x.getType()));
    TypeParser stringTypeParser = null;
    for (Schema schema : schemas) {
      TypeParser typeParser = registry.get(schema.getType());
      typeParser.errorMessage = new FormatErrorMessage("Parsing failed");
      typeParser.value = null;
      typeParser.analyze(schema, cell);
      if (schema.getType() == Schema.Type.STRING) {
        stringTypeParser = typeParser;
      } else {
        return typeParser;
      }
    }
    return stringTypeParser;
  }
}
