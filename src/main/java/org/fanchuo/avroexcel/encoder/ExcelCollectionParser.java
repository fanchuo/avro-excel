package org.fanchuo.avroexcel.encoder;

import java.util.*;
import org.apache.avro.Schema;
import org.apache.poi.ss.util.CellAddress;
import org.fanchuo.avroexcel.excelutil.CompositeErrorMessage;
import org.fanchuo.avroexcel.excelutil.FormatErrorMessage;

public abstract class ExcelCollectionParser<TSource, TTargetCollection, TIterable> {
  abstract TTargetCollection empty();

  abstract Schema.Type schemaType();

  abstract Schema subSchema(Schema schema);

  abstract Iterable<TIterable> iterable(TSource source);

  abstract ExcelRecord unwrapRecord(TIterable iterable);

  abstract void aggregate(TTargetCollection collection, TIterable item, Object value);

  public ParserResult parseCollection(TSource records, Schema schema, CellAddress address) {
    return ParserTools.parse(records, schema, this.schemaType(), this::seekMatch, address);
  }

  private ParserResult seekMatch(TSource records, Schema schema, CellAddress address) {
    TTargetCollection payload = this.empty();
    Schema subSchema = this.subSchema(schema);
    for (TIterable iterable : this.iterable(records)) {
      ExcelRecord excelRecord = this.unwrapRecord(iterable);
      if (excelRecord.candidates.containsKey(subSchema)) {
        Object value = excelRecord.candidates.get(subSchema);
        this.aggregate(payload, iterable, value);
      } else {
        CompositeErrorMessage compositeErrorMessage = new CompositeErrorMessage();
        compositeErrorMessage.add(
            new FormatErrorMessage("Failed to match schema %s", address, subSchema));
        compositeErrorMessage.add(excelRecord.failures.get(subSchema));
        return new ParserResult(compositeErrorMessage, null);
      }
    }
    return new ParserResult(null, payload);
  }

  public static final ExcelCollectionParser<List<ExcelRecord>, List<Object>, ExcelRecord>
      ARRAY_PARSER =
          new ExcelCollectionParser<>() {
            @Override
            List<Object> empty() {
              return new ArrayList<>();
            }

            @Override
            Schema.Type schemaType() {
              return Schema.Type.ARRAY;
            }

            @Override
            Schema subSchema(Schema schema) {
              return schema.getElementType();
            }

            @Override
            Iterable<ExcelRecord> iterable(List<ExcelRecord> excelRecords) {
              return excelRecords;
            }

            @Override
            ExcelRecord unwrapRecord(ExcelRecord excelRecord) {
              return excelRecord;
            }

            @Override
            void aggregate(List<Object> objects, ExcelRecord item, Object value) {
              objects.add(value);
            }
          };

  public static final ExcelCollectionParser<
          Map<String, ExcelRecord>, Map<String, Object>, Map.Entry<String, ExcelRecord>>
      MAP_PARSER =
          new ExcelCollectionParser<>() {
            @Override
            Map<String, Object> empty() {
              return new HashMap<>();
            }

            @Override
            Schema.Type schemaType() {
              return Schema.Type.MAP;
            }

            @Override
            Schema subSchema(Schema schema) {
              return schema.getValueType();
            }

            @Override
            Iterable<Map.Entry<String, ExcelRecord>> iterable(
                Map<String, ExcelRecord> stringExcelRecordMap) {
              return stringExcelRecordMap.entrySet();
            }

            @Override
            ExcelRecord unwrapRecord(Map.Entry<String, ExcelRecord> stringExcelRecordEntry) {
              return stringExcelRecordEntry.getValue();
            }

            @Override
            void aggregate(
                Map<String, Object> stringObjectMap,
                Map.Entry<String, ExcelRecord> item,
                Object value) {
              stringObjectMap.put(item.getKey(), value);
            }
          };
}
