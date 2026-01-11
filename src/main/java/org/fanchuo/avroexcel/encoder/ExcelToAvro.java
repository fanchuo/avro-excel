package org.fanchuo.avroexcel.encoder;

import java.util.*;
import java.util.stream.Collectors;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.fanchuo.avroexcel.excelutil.CompositeErrorMessage;
import org.fanchuo.avroexcel.excelutil.ErrorMessage;
import org.fanchuo.avroexcel.excelutil.ExcelSheetReader;
import org.fanchuo.avroexcel.excelutil.FormatErrorMessage;
import org.fanchuo.avroexcel.headerinfo.HeaderInfo;
import org.fanchuo.avroexcel.recordgeometry.RecordGeometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelToAvro {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExcelToAvro.class);

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
  private final ExcelFieldParser excelFieldParser = new ExcelFieldParser();
  private int row;

  public ExcelToAvro(
      ExcelSheetReader sheet, Schema schema, HeaderInfo headerInfo, int col, int row) {
    this.sheet = sheet;
    this.schema = schema;
    this.headerInfo = headerInfo;
    this.col = col;
    this.row = row;
  }

  public GenericRecord readRecord() throws ExcelSchemaException {
    Schema s = Schema.createUnion(this.schema, Schema.create(Schema.Type.NULL));
    ExcelRecord excelRecords =
        visitObject(this.col, this.row, Collections.singletonList(s), this.headerInfo);
    if (excelRecords.candidates.isEmpty()) {
      CellAddress address = new CellAddress(this.row, this.col);
      CompositeErrorMessage compositeErrorMessage = new CompositeErrorMessage();
      for (Map.Entry<Schema, ErrorMessage> entry : excelRecords.failures.entrySet()) {
        compositeErrorMessage.add(
            new FormatErrorMessage(
                "Cannot match schema %s", address, new SchemaReport(entry.getKey())));
        compositeErrorMessage.add(entry.getValue());
      }
      StringBuilder sb = new StringBuilder();
      compositeErrorMessage.dump("", sb);
      throw new ExcelSchemaException(sb.toString());
    }
    GenericRecord toReturn = (GenericRecord) excelRecords.candidates.values().iterator().next();
    this.row += excelRecords.recordGeometry.rowSpan;
    return toReturn;
  }

  private ExcelRecord visitScalar(int col, int row, List<Schema> schemas) {
    LOGGER.debug("visitScalar : col: {}, row: {}, schemas: {}", col, row, schemas);
    Cell c = sheet.getCell(col, row);
    Map<Schema, Object> excelRecords = new HashMap<>();
    Map<Schema, ErrorMessage> failure = new HashMap<>();
    for (Schema schema : schemas) {
      ExcelFieldParser.TypeParser typeParser =
          this.excelFieldParser.checkCompatible(schema, c, new CellAddress(row, col));
      if (typeParser.isCompatible()) {
        excelRecords.put(schema, typeParser.value);
      } else failure.put(schema, typeParser.errorMessage);
    }
    LOGGER.debug("return scalar - {}", excelRecords);
    return new ExcelRecord(excelRecords, failure, RecordGeometry.ATOM, false);
  }

  private int extractCollectionSize(int col, int row) {
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

  private enum Choice {
    UNDEF,
    MAP,
    ARRAY,
    RECORD,
    SCALAR,
  }

  private boolean checkNotBlank(int col, int row) {
    Cell cell = this.sheet.getCell(col, row);
    if (cell == null) return false;
    return cell.getCellType() != CellType.BLANK;
  }

  private ExcelRecord visitObject(int col, int row, List<Schema> schemas, HeaderInfo headerInfo) {
    LOGGER.debug(
        "visitObject : col: {}, row: {}, schemas: {}, headerInfo: {}",
        col,
        row,
        schemas,
        headerInfo);
    CellAddress address = new CellAddress(row, col);
    if (headerInfo.subHeaders == null) {
      if (checkNotBlank(col, row)) return visitScalar(col, row, schemas);
      return visitNull(schemas, address);
    }
    Map<String, ExcelRecord> subRecords = new HashMap<>();
    int colIdx = col;
    int arraySize = -1;
    CollectionDescriptor arrayCol = null;
    int mapSize = -1;
    CollectionDescriptor mapCol = null;
    CollectionDescriptor keyCol = null;
    CollectionDescriptor valueCol = null;
    List<Schema> recordSubSchemas =
        ParserTools.flatten(schemas, x -> x.getType() == Schema.Type.RECORD);
    for (HeaderInfo subHeader : headerInfo.subHeaders) {
      if ("*size".equals(subHeader.text)) {
        arraySize = extractCollectionSize(colIdx, row);
      } else if ("*".equals(subHeader.text)) {
        arrayCol = new CollectionDescriptor(colIdx, subHeader);
      } else if ("#size".equals(subHeader.text)) {
        mapSize = extractCollectionSize(colIdx, row);
      } else if ("#k".equals(subHeader.text)) {
        keyCol = new CollectionDescriptor(colIdx, subHeader);
      } else if ("#v".equals(subHeader.text)) {
        mapCol = new CollectionDescriptor(colIdx, subHeader);
      } else if (".value".equals(subHeader.text)) {
        valueCol = new CollectionDescriptor(colIdx, subHeader);
      } else {
        List<Schema> subSchema = new ArrayList<>();
        for (Schema schema : recordSubSchemas) {
          Schema.Field field = schema.getField(subHeader.text);
          if (field != null) subSchema.add(field.schema());
        }
        ExcelRecord field = visitObject(colIdx, row, subSchema, subHeader);
        subRecords.put(subHeader.text, field);
      }
      colIdx += subHeader.colSpan;
    }
    Choice choice = Choice.UNDEF;
    if (arraySize != -1 && arrayCol != null) {
      choice = Choice.ARRAY;
    }
    if (mapSize != -1 && mapCol != null && keyCol != null) {
      if (choice != Choice.UNDEF) return failsChoice(schemas, address, choice, Choice.MAP);
      choice = Choice.MAP;
    }
    if (valueCol != null && checkNotBlank(valueCol.col, row)) {
      if (choice != Choice.UNDEF) return failsChoice(schemas, address, choice, Choice.SCALAR);
      choice = Choice.SCALAR;
    }
    if (!checkEmpty(subRecords)) {
      if (choice != Choice.UNDEF) return failsChoice(schemas, address, choice, Choice.RECORD);
      choice = Choice.RECORD;
    }
    LOGGER.debug(
        "Choice is {}, arraySize: {}, arrayCol: {}, mapSize: {}, mapCol: {}, keyCol: {}, valueCol: {}, subRecords: {}",
        choice,
        arraySize,
        arrayCol,
        mapSize,
        mapCol,
        keyCol,
        valueCol,
        subRecords.keySet());
    switch (choice) {
      case ARRAY:
        return visitArray(row, arraySize, schemas, arrayCol);
      case MAP:
        return visitMap(row, mapSize, schemas, keyCol, mapCol);
      case SCALAR:
        return visitScalar(valueCol.col, row, schemas);
      case RECORD:
        return visitRecord(subRecords, schemas, address);
      default:
        return visitNull(schemas, address);
    }
  }

  private boolean checkEmpty(Map<String, ExcelRecord> subRecords) {
    for (ExcelRecord r : subRecords.values()) {
      if (!r.empty) return false;
    }
    return true;
  }

  private static ExcelRecord failsChoice(
      List<Schema> schemas, CellAddress address, Choice choice1, Choice choice2) {
    Map<Schema, ErrorMessage> failures = new HashMap<>();
    FormatErrorMessage formatErrorMessage =
        new FormatErrorMessage("Cannot be both %s and %s", address, choice1, choice2);
    for (Schema schema : schemas) {
      failures.put(schema, formatErrorMessage);
    }
    return new ExcelRecord(Collections.emptyMap(), failures, RecordGeometry.ATOM, false);
  }

  private ExcelRecord visitNull(List<Schema> schemas, CellAddress address) {
    LOGGER.debug("visitNull : schemas: {}", schemas);
    Map<Schema, Object> candidates = new HashMap<>();
    Map<Schema, ErrorMessage> failures = new HashMap<>();
    for (Schema schema : schemas) {
      CollectionTypes collectionTypes = ParserTools.collectTypes(schema);
      if (collectionTypes.nullable) {
        candidates.put(schema, null);
      } else if (collectionTypes.listable) {
        candidates.put(schema, Collections.emptyList());
      } else if (collectionTypes.mappable) {
        candidates.put(schema, Collections.emptyMap());
      } else {
        failures.put(schema, new FormatErrorMessage("Not a nullable data", address));
      }
    }
    LOGGER.debug("return null - {}", candidates);
    return new ExcelRecord(candidates, failures, RecordGeometry.ATOM, true);
  }

  private ExcelRecord visitRecord(
      Map<String, ExcelRecord> subRecords, List<Schema> schemas, CellAddress address) {
    LOGGER.debug("visitRecord : subRecords: {}, schemas: {}", subRecords, schemas);
    Map<Schema, Object> candidates = new HashMap<>();
    Map<Schema, ErrorMessage> failures = new HashMap<>();
    int rowSpan = 0;
    Map<String, RecordGeometry> map = new HashMap<>();
    for (Map.Entry<String, ExcelRecord> entry : subRecords.entrySet()) {
      rowSpan = Math.max(rowSpan, entry.getValue().recordGeometry.rowSpan);
      map.put(entry.getKey(), entry.getValue().recordGeometry);
    }
    for (Schema schema : schemas) {
      ParserResult recordParser = ExcelRecordParser.parseRecord(subRecords, schema, address);
      if (recordParser.errorMessage == null) {
        candidates.put(schema, recordParser.payload);
      } else {
        failures.put(schema, recordParser.errorMessage);
      }
    }
    LOGGER.debug("return record - {}", candidates);
    return new ExcelRecord(candidates, failures, new RecordGeometry(rowSpan, map, null), false);
  }

  private ExcelRecord visitArray(
      int row,
      int collectionSize,
      List<Schema> schemas,
      CollectionDescriptor collectionDescriptor) {
    LOGGER.debug(
        "visitArray : row: {}, collectionSize: {}, schemas: {}, collectionDescriptor: {}",
        row,
        collectionSize,
        schemas,
        collectionDescriptor);
    List<Schema> arraySchemas =
        ParserTools.flatten(schemas, x -> x.getType() == Schema.Type.ARRAY).stream()
            .map(Schema::getElementType)
            .collect(Collectors.toList());
    final int col = collectionDescriptor.col;
    CellAddress address = new CellAddress(row, col);
    final HeaderInfo headerInfo = collectionDescriptor.headerInfo;
    List<ExcelRecord> records = new ArrayList<>();
    int rowIdx = row;
    int rowSpan = 0;
    List<RecordGeometry> subList = new ArrayList<>();
    Map<Schema, Object> candidates = new HashMap<>();
    Map<Schema, ErrorMessage> failures = new HashMap<>();
    while (rowSpan < collectionSize) {
      ExcelRecord entry = visitObject(col, rowIdx, arraySchemas, headerInfo);
      subList.add(entry.recordGeometry);
      rowIdx += entry.recordGeometry.rowSpan;
      rowSpan += entry.recordGeometry.rowSpan;
      records.add(entry);
    }
    for (Schema schema : schemas) {
      ParserResult arrayParser =
          ExcelCollectionParser.ARRAY_PARSER.parseCollection(records, schema, address);
      if (arrayParser.errorMessage == null) {
        candidates.put(schema, arrayParser.payload);
      } else {
        failures.put(schema, arrayParser.errorMessage);
      }
    }
    LOGGER.debug("return array - {}", candidates);
    return new ExcelRecord(candidates, failures, new RecordGeometry(rowSpan, null, subList), false);
  }

  private ExcelRecord visitMap(
      int row,
      int collectionSize,
      List<Schema> schemas,
      CollectionDescriptor keyDesc,
      CollectionDescriptor valueDesc) {
    LOGGER.debug(
        "visitMap : row: {}, collectionSize: {}, schemas: {}, keyDesc: {}, valueDesc: {}",
        row,
        collectionSize,
        schemas,
        keyDesc,
        valueDesc);
    List<Schema> mapSchemas =
        ParserTools.flatten(schemas, x -> x.getType() == Schema.Type.MAP).stream()
            .map(Schema::getValueType)
            .collect(Collectors.toList());
    final int keyCol = keyDesc.col;
    final int valCol = valueDesc.col;
    CellAddress address = new CellAddress(row, valCol);
    final HeaderInfo headerInfo = valueDesc.headerInfo;
    Map<String, ExcelRecord> records = new HashMap<>();
    int rowIdx = row;
    int rowSpan = 0;
    List<RecordGeometry> subList = new ArrayList<>();
    while (rowSpan < collectionSize) {
      String k = sheet.getCell(keyCol, rowIdx).toString();
      ExcelRecord entry = visitObject(valCol, rowIdx, mapSchemas, headerInfo);
      subList.add(entry.recordGeometry);
      rowIdx += entry.recordGeometry.rowSpan;
      rowSpan += entry.recordGeometry.rowSpan;
      records.put(k, entry);
    }
    Map<Schema, Object> candidates = new HashMap<>();
    Map<Schema, ErrorMessage> failures = new HashMap<>();
    for (Schema schema : schemas) {
      ParserResult arrayParser =
          ExcelCollectionParser.MAP_PARSER.parseCollection(records, schema, address);
      if (arrayParser.errorMessage == null) {
        candidates.put(schema, arrayParser.payload);
      } else {
        failures.put(schema, arrayParser.errorMessage);
      }
    }
    LOGGER.debug("return map - {}", candidates);
    return new ExcelRecord(candidates, failures, new RecordGeometry(rowSpan, null, subList), false);
  }
}
