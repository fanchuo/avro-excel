package org.fanchuo.avroexcel.converters;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.util.CellAddress;
import org.fanchuo.avroexcel.encoder.ParserTools;
import org.fanchuo.avroexcel.excelutil.BytesUtils;
import org.fanchuo.avroexcel.excelutil.FormatErrorMessage;
import org.fanchuo.avroexcel.excelutil.TimestampParser;

public class ExcelFieldParser implements IExcelFieldParser {

  private abstract static class TypeParser {
    public abstract ParserResult analyze(Schema schema, Cell cell, CellAddress address);
  }

  static class EnumExcelFieldParser extends TypeParser {
    @Override
    public ParserResult analyze(Schema schema, Cell cell, CellAddress address) {
      if (cell.getCellType() == CellType.STRING) {
        String str = cell.getStringCellValue();
        if (schema.getEnumSymbols().contains(str)) {
          return new ParserResult(null, new GenericData.EnumSymbol(schema, str));
        }
        return new ParserResult(
            new FormatErrorMessage("'%s' is not one of %s", address, str, schema.getEnumSymbols()),
            null);
      }
      return new ParserResult(
          new FormatErrorMessage("Cell type '%s' is not STRING", address, cell.getCellType()),
          null);
    }
  }

  static class StringExcelFieldParser extends TypeParser {
    @Override
    public ParserResult analyze(Schema schema, Cell cell, CellAddress address) {
      if (cell.getCellType() == CellType.STRING) {
        return new ParserResult(null, cell.getStringCellValue());
      }
      return new ParserResult(
          new FormatErrorMessage("Cell type '%s' is not STRING", address, cell.getCellType()),
          null);
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

  abstract static class AbstractIntExcelFieldParser<T extends Number> extends TypeParser {
    abstract T getIntValue(double v);

    @Override
    public ParserResult analyze(Schema schema, Cell cell, CellAddress address) {
      String logicalType =
          schema.getLogicalType() == null ? null : schema.getLogicalType().getName();
      if (logicalType != null && LOCALDATE_LOGICAL_TYPES.contains(logicalType)) {
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
          if ("date".equals(logicalType))
            return new ParserResult(null, cell.getLocalDateTimeCellValue().toLocalDate());
          if (logicalType.startsWith("time-"))
            return new ParserResult(null, cell.getLocalDateTimeCellValue().toLocalTime());
          return new ParserResult(null, cell.getLocalDateTimeCellValue());
        }
        return new ParserResult(
            new FormatErrorMessage(
                "Not a date cell type (type: %s, format: %s)",
                address, cell.getCellType(), cell.getCellStyle().getDataFormat()),
            null);
      }
      if (TIMESTAMP_LOGICAL_TYPES.contains(logicalType)) {
        if (cell.getCellType() == CellType.STRING) {
          Instant instant = TimestampParser.parseDate(cell);
          if (instant != null) {
            return new ParserResult(null, instant);
          }
          return new ParserResult(
              new FormatErrorMessage("Cell format '%s' is not ISO8601 format", address, cell),
              null);
        }
        return new ParserResult(
            new FormatErrorMessage("Cell type '%s' is not STRING", address, cell.getCellType()),
            null);
      }
      if (cell.getCellType() == CellType.NUMERIC) {
        return new ParserResult(null, getIntValue(cell.getNumericCellValue()));
      }
      return new ParserResult(
          new FormatErrorMessage("Cell type '%s' is not NUMERIC", address, cell.getCellType()),
          null);
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
    public ParserResult analyze(Schema schema, Cell cell, CellAddress address) {
      if (cell.getCellType() == CellType.NUMERIC) {
        return new ParserResult(null, getFloatValue(cell.getNumericCellValue()));
      }
      return new ParserResult(
          new FormatErrorMessage("Cell type '%s' is not NUMERIC", address, cell.getCellType()),
          null);
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
    public ParserResult analyze(Schema schema, Cell cell, CellAddress address) {
      if (cell.getCellType() == CellType.BOOLEAN || cell.getCellType() == CellType.FORMULA) {
        return new ParserResult(null, cell.getBooleanCellValue());
      }
      return new ParserResult(
          new FormatErrorMessage("Cell type '%s' is not BOOLEAN", address, cell.getCellType()),
          null);
    }
  }

  static class BytesExcelFieldParser extends TypeParser {
    @Override
    public ParserResult analyze(Schema schema, Cell cell, CellAddress address) {
      if (cell.getCellType() == CellType.STRING) {
        try {
          return new ParserResult(null, BytesUtils.stringToBytes(cell.getStringCellValue()));
        } catch (IllegalArgumentException e) {
          return new ParserResult(
              new FormatErrorMessage(
                  "Cell value '%s' is not base 64 encoded", address, cell.getStringCellValue()),
              null);
        }
      }
      return new ParserResult(
          new FormatErrorMessage("Cell type '%s' is not a string", address, cell.getCellType()),
          null);
    }
  }

  private final EnumMap<Schema.Type, TypeParser> registry = new EnumMap<>(Schema.Type.class);

  public ExcelFieldParser() {
    registry.put(Schema.Type.ENUM, new EnumExcelFieldParser());
    registry.put(Schema.Type.STRING, new StringExcelFieldParser());
    registry.put(Schema.Type.INT, new IntExcelFieldParser());
    registry.put(Schema.Type.LONG, new LongExcelFieldParser());
    registry.put(Schema.Type.FLOAT, new FloatExcelFieldParser());
    registry.put(Schema.Type.DOUBLE, new DoubleExcelFieldParser());
    registry.put(Schema.Type.BOOLEAN, new BooleanExcelFieldParser());
    registry.put(Schema.Type.BYTES, new BytesExcelFieldParser());
  }

  public ParserResult checkCompatible(Schema s, Cell cell, CellAddress address) {
    List<Schema> schemas = ParserTools.flatten(s, x -> registry.containsKey(x.getType()));
    ParserResult stringTypeParser = null;
    for (Schema schema : schemas) {
      TypeParser typeParser = registry.get(schema.getType());
      ParserResult result = typeParser.analyze(schema, cell, address);
      if (schema.getType() == Schema.Type.STRING) {
        stringTypeParser = result;
      } else {
        return result;
      }
    }
    return stringTypeParser;
  }

  @Override
  public Type guessType(Cell cell, CellAddress address) throws InferSchemaException {
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
        throw new InferSchemaException(
            String.format(
                "Cannot encode value '%s' of type '%s' in cell '%s'",
                cell, cell.getCellStyle(), address));
    }
  }
}
