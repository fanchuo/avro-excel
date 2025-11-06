package org.fanchuo.avroexcel.encoder;

import org.apache.avro.Schema;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;

import java.text.ParsePosition;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

public class ExcelFieldParser {
    public static abstract class TypeParser {
        public boolean compatible;
        public Object value;

        public abstract void analyze(Schema schema, Cell cell);
    }

    static class EnumExcelFieldParser extends TypeParser {
        @Override
        public void analyze(Schema schema, Cell cell) {
            if (cell.getCellType() == CellType.STRING) {
                String str = cell.getStringCellValue();
                if (schema.getEnumSymbols().contains(str)) {
                    this.compatible = true;
                    this.value = str;
                }
            }
        }
    }

    static class StringExcelFieldParser extends TypeParser {
        @Override
        public void analyze(Schema schema, Cell cell) {
            this.compatible = cell.getCellType()==CellType.STRING;
            this.value = cell.getStringCellValue();
        }
    }

    private static final Set<String> LOCALDATE_LOGICAL_TYPES = new CopyOnWriteArraySet<>(Arrays.asList(
            "date",
            "time-millis",
            "time-micros",
            "local-timestamp-millis",
            "local-timestamp-micros",
            "local-timestamp-nanos"
    ));

    private static final Set<String> TIMESTAMP_LOGICAL_TYPES = new CopyOnWriteArraySet<>(Arrays.asList(
            "timestamp-millis",
            "timestamp-micros",
            "timestamp-nanos"
    ));

    /**
     * @param cell An Excel cell
     * @return If it is formatted as a date
     * @see org.apache.poi.ss.usermodel.BuiltinFormats
     */
    private static boolean isDate(Cell cell) {
        CellStyle style = cell.getCellStyle();
        if (style==null) return false;
        short dataFormat = style.getDataFormat();
        return dataFormat>=0xe && dataFormat<=0x16;
    }

    static abstract class AbstractIntExcelFieldParser<T extends Number> extends TypeParser {
        abstract T getIntValue(double v);

        @Override
        public void analyze(Schema schema, Cell cell) {
            String logicalType = schema.getLogicalType()==null?null:schema.getLogicalType().getName();
            if (LOCALDATE_LOGICAL_TYPES.contains(logicalType)) {
                if (cell.getCellType()==CellType.NUMERIC && isDate(cell)) {
                    this.compatible = true;
                    this.value = cell.getLocalDateTimeCellValue();
                }
            }
            else if (TIMESTAMP_LOGICAL_TYPES.contains(logicalType)) {
                if (cell.getCellType()==CellType.STRING) {
                    String str = cell.getStringCellValue();
                    ParsePosition position = new ParsePosition(0);
                    TemporalAccessor temporalAccessor = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parseUnresolved(str, position);
                    if(position.getErrorIndex()<0) {
                        this.compatible = true;
                        this.value = temporalAccessor.query(Instant::from);
                    }
                }
            } else if (cell.getCellType()==CellType.NUMERIC) {
                this.compatible = true;
                this.value = getIntValue(cell.getNumericCellValue());
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

    static abstract class AbstractFloatExcelFieldParser<T extends Number> extends TypeParser {
        abstract T getFloatValue(double v);

        @Override
        public void analyze(Schema schema, Cell cell) {
            if (cell.getCellType()==CellType.NUMERIC) {
                this.compatible = true;
                this.value = getFloatValue(cell.getNumericCellValue());
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
            if (cell.getCellType()==CellType.BOOLEAN) {
                this.compatible = true;
                this.value = cell.getBooleanCellValue();
            }
        }
    }

    static class NullExcelFieldParser extends TypeParser {
        @Override
        public void analyze(Schema schema, Cell cell) {}
    }

    private static final NullExcelFieldParser  NULL_PARSER = new NullExcelFieldParser();
    private static final EnumMap<Schema.Type, TypeParser> registry = new EnumMap<>(Schema.Type.class);
    static {
        registry.put(Schema.Type.ENUM, new EnumExcelFieldParser());
        registry.put(Schema.Type.STRING, new StringExcelFieldParser());
        registry.put(Schema.Type.INT, new IntExcelFieldParser());
        registry.put(Schema.Type.LONG, new LongExcelFieldParser());
        registry.put(Schema.Type.FLOAT, new FloatExcelFieldParser());
        registry.put(Schema.Type.DOUBLE, new DoubleExcelFieldParser());
        registry.put(Schema.Type.BOOLEAN, new BooleanExcelFieldParser());
        registry.put(Schema.Type.NULL, NULL_PARSER);
    }

    public static TypeParser checkCompatible(Schema s, Cell cell) {
        List<Schema> schemas = ParserTools.flatten(s, x->true);
        TypeParser stringTypeParser = null;
        for (Schema schema : schemas) {
            TypeParser typeParser = registry.getOrDefault(schema.getType(), NULL_PARSER);
            typeParser.compatible = false;
            typeParser.value = null;
            if (schema.getType()==Schema.Type.NULL) {
                if (cell==null || cell.getCellType()==CellType.BLANK) typeParser.compatible = true;
            } else if (cell!=null) {
                typeParser.analyze(schema, cell);
            }
            if (schema.getType()==Schema.Type.STRING) {
                stringTypeParser = typeParser;
            } else {
                return typeParser;
            }
        }
        return stringTypeParser;
    }
}

