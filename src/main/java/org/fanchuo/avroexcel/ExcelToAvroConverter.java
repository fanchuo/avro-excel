package org.fanchuo.avroexcel;

import java.io.*;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.fanchuo.avroexcel.encoder.ExcelSchemaException;
import org.fanchuo.avroexcel.encoder.ExcelToAvro;
import org.fanchuo.avroexcel.excelutil.ExcelSheetReader;
import org.fanchuo.avroexcel.headerinfo.HeaderInfo;
import org.fanchuo.avroexcel.headerinfo.HeaderInfoExcelReader;

public class ExcelToAvroConverter {
  public static void convert(
      File excelFile,
      OutputStream avroOutputStream,
      String sheetName,
      int col,
      int row,
      Schema schema)
      throws IOException, ExcelSchemaException {
    try (InputStream is = new FileInputStream(excelFile)) {
      convert(is, avroOutputStream, sheetName, col, row, schema);
    }
  }

  public static void convert(
      InputStream inputStream,
      OutputStream avroOutputStream,
      String sheetName,
      int col,
      int row,
      Schema schema)
      throws IOException, ExcelSchemaException {
    ExcelSheetReader excelSheetReader = ExcelSheetReader.loadSheet(inputStream, sheetName);
    HeaderInfo headerInfo = HeaderInfoExcelReader.visitSheet(excelSheetReader, col, row);
    ExcelToAvro excelToAvro =
        new ExcelToAvro(excelSheetReader, schema, headerInfo, col, row + headerInfo.rowSpan);
    GenericRecord record;
    GenericData genericData = AvroReader.makeGenericData();
    DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(schema, genericData);
    try (DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<>(datumWriter)) {
      dataFileWriter.create(schema, avroOutputStream);
      while ((record = excelToAvro.readRecord()) != null) {
        dataFileWriter.append(record);
      }
    }
  }
}
