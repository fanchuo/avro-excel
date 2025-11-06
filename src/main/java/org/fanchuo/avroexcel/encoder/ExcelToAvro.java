package org.fanchuo.avroexcel.encoder;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.fanchuo.avroexcel.AvroReader;
import org.fanchuo.avroexcel.excelutil.ExcelSheetReader;
import org.fanchuo.avroexcel.headerinfo.HeaderInfo;
import org.fanchuo.avroexcel.recordgeometry.RecordGeometry;
import java.util.*;
import java.util.stream.Collectors;

public class ExcelToAvro {
    private static class CollectionDescriptor {
        final int col;
        final HeaderInfo headerInfo;

        CollectionDescriptor(int col, HeaderInfo headerInfo) {
            this.col = col;
            this.headerInfo = headerInfo;
        }
    }

    private final ExcelSheetReader sheet;
    private final Schema schema;
    private final HeaderInfo headerInfo;
    private final int col;
    private int row;
    private final GenericData genericData = AvroReader.makeGenericData();

    public ExcelToAvro(ExcelSheetReader sheet, Schema schema, HeaderInfo headerInfo, int col, int row) {
        this.sheet = sheet;
        this.schema = schema;
        this.headerInfo = headerInfo;
        this.col = col;
        this.row = row;
    }

    public GenericRecord readRecord() {
        ExcelRecord excelRecords = visitObject(this.col, this.row, Collections.singletonList(this.schema), this.headerInfo);
        if (excelRecords.candidates.isEmpty()) return null;
        GenericRecord toReturn = (GenericRecord) excelRecords.candidates.values().iterator().next();
        this.row += excelRecords.recordGeometry.rowSpan;
        return toReturn;
    }

    private ExcelRecord visitScalar(int col, int row, List<Schema> schemas) {
        Cell c = sheet.getCell(col, row);
        Map<Schema, Object> excelRecords = new HashMap<>();
        for (Schema schema : schemas) {
            ExcelFieldParser.TypeParser typeParser = ExcelFieldParser.checkCompatible(schema, c);
            if (typeParser.compatible) excelRecords.put(schema, typeParser.value);
        }
        return new ExcelRecord(excelRecords, RecordGeometry.ATOM);
    }

    private ExcelRecord visitObject(int col, int row, List<Schema> schemas, HeaderInfo headerInfo) {
        if (headerInfo.subHeaders==null) {
            return visitScalar(col, row, schemas);
        }
        Map<String, ExcelRecord> subRecords = new HashMap<>();
        int colIdx = col;
        int arraySize = -1;
        CollectionDescriptor arrayCol = null;
        int mapSize = -1;
        CollectionDescriptor mapCol = null;
        CollectionDescriptor keyCol = null;
        List<Schema> recordSubSchemas = ParserTools.flatten(schemas, x->x.getType() == Schema.Type.RECORD);
        for (HeaderInfo subHeader : headerInfo.subHeaders) {
            if ("*size".equals(subHeader.text)) {
                Cell c = sheet.getCell(colIdx, row);
                if (c!=null && c.getCellType() == CellType.NUMERIC) {
                    arraySize = (int) Math.round(c.getNumericCellValue());
                }
            } else if ("*".equals(subHeader.text)) {
                arrayCol = new CollectionDescriptor(colIdx, subHeader);
            } else if ("#size".equals(subHeader.text)) {
                Cell c = sheet.getCell(colIdx, row);
                if (c!=null && c.getCellType() == CellType.NUMERIC) {
                    mapSize = (int) Math.round(c.getNumericCellValue());
                }
            } else if ("#k".equals(subHeader.text)) {
                keyCol = new CollectionDescriptor(colIdx, subHeader);
            } else if ("#v".equals(subHeader.text)) {
                mapCol = new CollectionDescriptor(colIdx, subHeader);
            } else if (".value".equals(subHeader.text)) {
                ExcelRecord scalar = visitScalar(colIdx, row, schemas);
                if (!scalar.isNull()) return scalar;
            } else {
                List<Schema> subSchema = new ArrayList<>();
                for (Schema schema : recordSubSchemas) {
                    Schema.Field field = schema.getField(subHeader.text);
                    if (field!=null) subSchema.add(field.schema());
                }
                ExcelRecord field = visitObject(colIdx, row, subSchema, subHeader);
                if (!field.isNull() && !field.candidates.isEmpty()) subRecords.put(subHeader.text, field);
            }
            colIdx += subHeader.colSpan;
        }
        if (arraySize!=-1 && arrayCol!=null) {
            return visitArray(row, arraySize, schemas, arrayCol);
        }
        if (mapSize!=-1 && mapCol!=null && keyCol!=null) {
            return visitMap(row, mapSize, schemas, keyCol, mapCol);
        }
        return visitRecord(subRecords, schemas);
    }

    private ExcelRecord visitRecord(Map<String, ExcelRecord> subRecords, List<Schema> schemas) {
        Map<Schema, Object> candidates = new HashMap<>();
        int rowSpan = 0;
        Map<String, RecordGeometry> map = new HashMap<>();
        for (Map.Entry<String, ExcelRecord> entry : subRecords.entrySet()) {
            rowSpan = Math.max(rowSpan, entry.getValue().recordGeometry.rowSpan);
            map.put(entry.getKey(), entry.getValue().recordGeometry);
        }
        for (Schema schema : schemas) {
            ExcelRecordParser.RecordParser recordParser = ExcelRecordParser.parseRecord(subRecords, schema);
            if (recordParser.compatible) {
                GenericRecord record = recordParser.payload;
                candidates.put(schema, record);
            }
        }
        return new ExcelRecord(candidates, new RecordGeometry(rowSpan, map, null));
    }

    private ExcelRecord visitArray(int row, int collectionSize, List<Schema> schemas, CollectionDescriptor collectionDescriptor) {
        List<Schema> arraySchemas = ParserTools.flatten(schemas, x -> x.getType() == Schema.Type.ARRAY)
                .stream().map(Schema::getElementType).collect(Collectors.toList());
        final int col = collectionDescriptor.col;
        final HeaderInfo headerInfo = collectionDescriptor.headerInfo;
        List<ExcelRecord> records = new ArrayList<>();
        int rowIdx = row;
        int rowSpan = 0;
        List<RecordGeometry> subList = new ArrayList<>();
        Map<Schema, Object> candidates = new HashMap<>();
        for (int i = 0; i< collectionSize; i++) {
            ExcelRecord entry = visitObject(col, rowIdx, arraySchemas, headerInfo);
            subList.add(entry.recordGeometry);
            rowIdx += entry.recordGeometry.rowSpan;
            rowSpan += entry.recordGeometry.rowSpan;
            records.add(entry);
        }
        for (Schema schema : schemas) {
            ExcelArrayParser.ArrayParser arrayParser = ExcelArrayParser.parseArray(records, schema);
            if (arrayParser.compatible) {
                candidates.put(schema, arrayParser.payload);
            }
        }
        return new ExcelRecord(candidates, new RecordGeometry(rowSpan, null, subList));
    }

    private ExcelRecord visitMap(int row, int collectionSize, List<Schema> schemas, CollectionDescriptor keyDesc, CollectionDescriptor valueDesc) {
        List<Schema> mapSchemas = ParserTools.flatten(schemas, x -> x.getType() == Schema.Type.MAP)
                .stream().map(Schema::getValueType).collect(Collectors.toList());
        final int keyCol = keyDesc.col;
        final int valCol = valueDesc.col;
        final HeaderInfo headerInfo = valueDesc.headerInfo;
        Map<String, ExcelRecord> records = new HashMap<>();
        int rowIdx = row;
        int rowSpan = 0;
        List<RecordGeometry> subList = new ArrayList<>();
        for (int i = 0; i< collectionSize; i++) {
            String k = sheet.getCell(keyCol, rowIdx).toString();
            ExcelRecord entry = visitObject(valCol, rowIdx, mapSchemas, headerInfo);
            subList.add(entry.recordGeometry);
            rowIdx += entry.recordGeometry.rowSpan;
            rowSpan += entry.recordGeometry.rowSpan;
            records.put(k, entry);
        }
        Map<Schema, Object> candidates = new HashMap<>();
        for (Schema schema : schemas) {
            ExcelMapParser.MapParser arrayParser = ExcelMapParser.parseMap(records, schema);
            if (arrayParser.compatible) {
                candidates.put(schema, arrayParser.payload);
            }
        }
        return new ExcelRecord(candidates, new RecordGeometry(rowSpan, null, subList));
    }
}
