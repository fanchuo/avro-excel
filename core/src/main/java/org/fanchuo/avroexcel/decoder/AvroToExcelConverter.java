package org.fanchuo.avroexcel.decoder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.avro.generic.GenericRecord;
import org.fanchuo.avroexcel.converters.*;

public class AvroToExcelConverter {
  private static String makeSheetname(String sheetName) {
    if (sheetName == null) return "tab";
    return sheetName;
  }

  public static void convert(
      File avroFile, File excelFile, String sheetName, int col, int row, IConverters converters)
      throws IOException, DecoderSchemaException {
    try (AvroReader avroReader = new AvroReader(avroFile, converters.getGenericData());
        ExcelWriter workbookWriter =
            new ExcelWriter(
                excelFile,
                makeSheetname(sheetName),
                converters.getExcelFieldFormater(),
                col,
                row)) {
      convert(avroReader, workbookWriter);
    }
  }

  public static void convert(
      InputStream avroStream,
      OutputStream excelStream,
      String sheetName,
      int col,
      int row,
      IConverters converters)
      throws IOException, DecoderSchemaException {
    try (AvroReader avroReader = new AvroReader(avroStream, converters.getGenericData());
        ExcelWriter workbookWriter =
            new ExcelWriter(
                excelStream,
                makeSheetname(sheetName),
                converters.getExcelFieldFormater(),
                col,
                row)) {
      convert(avroReader, workbookWriter);
    }
  }

  private static void convert(
      GenericRecordIterator avroReader, GenericRecordConsumer workbookWriter)
      throws DecoderSchemaException, IOException {
    GenericRecord record;
    workbookWriter.declareSchema(avroReader.getSchema());
    while ((record = avroReader.readRecord()) != null) {
      workbookWriter.writeRecord(record);
    }
  }

  private static GenericRecordConsumer encodeExcel(
      File excelFile, String sheetName, int col, int row, IConverters converters)
      throws IOException {
    return new ExcelWriter(excelFile, sheetName, converters.getExcelFieldFormater(), col, row);
  }

  private static GenericRecordConsumer encodeExcel(
      OutputStream excelStream, String sheetName, int col, int row, IConverters converters)
      throws IOException {
    return new ExcelWriter(excelStream, sheetName, converters.getExcelFieldFormater(), col, row);
  }

  private static GenericRecordIterator decodeAvro(File avrofile, IConverters converters)
      throws IOException {
    return new AvroReader(avrofile, converters.getGenericData());
  }

  private static GenericRecordIterator decodeAvro(InputStream avroStream, IConverters converters)
      throws IOException {
    return new AvroReader(avroStream, converters.getGenericData());
  }
}
