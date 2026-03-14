package org.fanchuo.avroexcel.encoder;

import java.io.*;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.fanchuo.avroexcel.converters.DecoderSchemaException;
import org.fanchuo.avroexcel.converters.GenericRecordConsumer;
import org.fanchuo.avroexcel.converters.GenericRecordIterator;
import org.fanchuo.avroexcel.converters.IConverters;
import org.fanchuo.avroexcel.excelutil.ExcelSheetReader;
import org.fanchuo.avroexcel.headerinfo.HeaderInfo;
import org.fanchuo.avroexcel.headerinfo.HeaderInfoExcelReader;

public class ExcelToAvroConverter {
  public static void convert(
      File excelFile,
      File avroFile,
      String sheetName,
      int col,
      int row,
      Schema schema,
      IConverters converters)
      throws IOException, DecoderSchemaException {
    try (InputStream is = new FileInputStream(excelFile);
        OutputStream os = new FileOutputStream(avroFile)) {
      convert(is, os, sheetName, col, row, schema, converters);
    }
  }

  public static void convert(
      InputStream inputStream,
      OutputStream avroOutputStream,
      String sheetName,
      int col,
      int row,
      Schema schema,
      IConverters converters)
      throws IOException, DecoderSchemaException {
    try (GenericRecordConsumer recordConsumer = encodeAvro(avroOutputStream, converters);
        GenericRecordIterator recordIterator =
            decodeExcel(inputStream, sheetName, col, row, schema, converters); ) {
      GenericRecord record;
      recordConsumer.declareSchema(recordIterator.getSchema());
      while ((record = recordIterator.readRecord()) != null) {
        recordConsumer.writeRecord(record);
      }
    }
  }

  public static GenericRecordConsumer encodeAvro(
      OutputStream avroOutputStream, IConverters converters) {
    return new AvroWriter(converters.getGenericData(), avroOutputStream);
  }

  public static ExcelReader decodeExcel(
      InputStream inputStream,
      String sheetName,
      int col,
      int row,
      Schema schema,
      IConverters converters)
      throws IOException {
    ExcelSheetReader excelSheetReader = ExcelSheetReader.loadSheet(inputStream, sheetName);
    HeaderInfo headerInfo = HeaderInfoExcelReader.visitSheet(excelSheetReader, col, row);
    return new ExcelReader(
        converters.getExcelFieldParser(),
        excelSheetReader,
        schema,
        headerInfo,
        col,
        row + headerInfo.rowSpan);
  }
}
