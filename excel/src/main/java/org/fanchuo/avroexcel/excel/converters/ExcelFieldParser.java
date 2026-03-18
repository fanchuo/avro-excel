package org.fanchuo.avroexcel.excel.converters;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.util.TimePeriod;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.util.CellAddress;
import org.fanchuo.avroexcel.core.avroutil.BytesUtils;
import org.fanchuo.avroexcel.core.avroutil.FormatErrorMessage;
import org.fanchuo.avroexcel.core.avroutil.ParserResult;
import org.fanchuo.avroexcel.core.avroutil.TimestampParser;
import org.fanchuo.avroexcel.core.avroutil.Type;
import org.fanchuo.avroexcel.excel.decoder.ParserTools;
import org.fanchuo.avroexcel.excel.infer.InferSchemaException;

public class ExcelFieldParser implements IExcelFieldParser {

  private abstract static class TypeParser {
    public abstract ParserResult analyze(Schema schema, Cell cell, CellAddress address);
  }

  abstract static class StringEncodedParser extends TypeParser {
    @Override
    public final ParserResult analyze(Schema schema, Cell cell, CellAddress address) {
      if (cell.getCellType() == CellType.STRING || cell.getCellType() == CellType.BLANK) {
        return analyzeStr(schema, cell.getStringCellValue(), address);
      }
      if (cell.getCellType() == CellType.NUMERIC) {
        return analyzeStr(schema, String.valueOf(cell.getNumericCellValue()), address);
      }
      return new ParserResult(
          new FormatErrorMessage("Cell type '%s' is not a string", address, cell.getCellType()),
          null);
    }

    public abstract ParserResult analyzeStr(Schema schema, String value, CellAddress address);
  }

  static class EnumExcelFieldParser extends StringEncodedParser {
    @Override
    public ParserResult analyzeStr(Schema schema, String value, CellAddress address) {
      if (schema.getEnumSymbols().contains(value)) {
        return new ParserResult(null, new GenericData.EnumSymbol(schema, value));
      }
      return new ParserResult(
          new FormatErrorMessage("'%s' is not one of %s", address, value, schema.getEnumSymbols()),
          null);
    }
  }

  static class StringExcelFieldParser extends StringEncodedParser {
    @Override
    public ParserResult analyzeStr(Schema schema, String value, CellAddress address) {
      if (schema.getLogicalType() instanceof LogicalTypes.Uuid) {
        try {
          return new ParserResult(null, UUID.fromString(value));
        } catch (IllegalArgumentException e) {
          return new ParserResult(new FormatErrorMessage("'%s' is not UUID", address, value), null);
        }
      }
      return new ParserResult(null, value);
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
          Instant instant = TimestampParser.parseDate(cell.getStringCellValue());
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

  static class BytesExcelFieldParser extends StringEncodedParser {
    @Override
    public ParserResult analyzeStr(Schema schema, String value, CellAddress address) {
      if (schema.getLogicalType() instanceof LogicalTypes.BigDecimal) {
        try {
          return new ParserResult(null, new BigDecimal(value));
        } catch (NumberFormatException e) {
          return new ParserResult(
              new FormatErrorMessage("Cell value '%s' is not a valid decimal", address, value),
              null);
        }
      }
      try {
        return new ParserResult(null, BytesUtils.stringToBytes(value));
      } catch (IllegalArgumentException e) {
        return new ParserResult(
            new FormatErrorMessage("Cell value '%s' is not base 64 encoded", address, value), null);
      }
    }
  }

  static class FixedExcelFieldParser extends StringEncodedParser {
    @Override
    public ParserResult analyzeStr(Schema schema, String value, CellAddress address) {
      if (schema.getLogicalType() instanceof LogicalTypes.Duration) {
        try {
          return new ParserResult(null, TimePeriod.from(Duration.parse(value)));
        } catch (DateTimeParseException e) {
          return new ParserResult(
              new FormatErrorMessage("Cell value '%s' is not an ISO-8601 duration", address, value),
              null);
        }
      }
      if (schema.getLogicalType() instanceof LogicalTypes.Decimal) {
        try {
          return new ParserResult(null, new BigDecimal(value));
        } catch (NumberFormatException e) {
          return new ParserResult(
              new FormatErrorMessage("Cell value '%s' is not a valid decimal", address, value),
              null);
        }
      }
      try {
        return new ParserResult(
            null, new GenericData.Fixed(schema, BytesUtils.stringToByteArray(value)));
      } catch (IllegalArgumentException e) {
        return new ParserResult(
            new FormatErrorMessage("Cell value '%s' is not base 64 encoded", address, value), null);
      }
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
    registry.put(Schema.Type.FIXED, new FixedExcelFieldParser());
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
        Instant instant = TimestampParser.parseDate(cell.getStringCellValue());
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
