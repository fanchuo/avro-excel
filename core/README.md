# Avro Excel Core

This module is the core of the conversion library between Avro and Excel formats. It provides the necessary tools to convert, validate, and manipulate data between these two formats.

## Main Goal

The main goal of this project is to facilitate data exchange between systems using Avro as a serialization format and users who prefer to handle data in spreadsheets like Excel. A major feature is the ability to **validate an Excel file** against an Avro schema, thus ensuring data integrity before processing.

## Features

### 1. Avro to Excel Conversion

Easily transform your data from Avro to Excel format. The library handles the creation of a tabular representation of your data, including complex types like nested records, arrays, and unions.

**Example:**
```java
import org.fanchuo.avroexcel.decoder.AvroToExcelConverter;
import org.fanchuo.avroexcel.converters.DefaultConverters;
import java.io.File;

// ...

File avroFile = new File("users.avro");
File excelFile = new File("users.xlsx");

AvroToExcelConverter.convert(avroFile, excelFile, "Avro Data", 1, 2, new DefaultConverters());
```

### 2. Excel to Avro Conversion

Convert an Excel file to an Avro file based on a predefined Avro schema. This is particularly useful for importing manually entered data into an Avro-based system.

**Example:**
```java
import org.fanchuo.avroexcel.encoder.ExcelToAvroConverter;
import org.fanchuo.avroexcel.converters.DefaultConverters;
import org.apache.avro.Schema;
import java.io.File;

// ...

File excelFile = new File("users.xlsx");
File avroFile = new File("users.avro");
Schema schema = new Schema.Parser().parse(new File("user.avsc"));

ExcelToAvroConverter.convert(excelFile, avroFile, "Avro Data", 1, 2, schema, new DefaultConverters());
```

### 3. Excel File Validation

The most powerful feature of this module is the validation of an Excel file's content against an Avro schema. If the Excel file does not comply with the schema (incorrect data types, missing or extra fields, etc.), an `ExcelSchemaException` is thrown with detailed information to locate the error.

**Error detection example:**
```java
// Attempts to convert an Excel file with an incorrect data type
try {
    ExcelToAvroConverter.convert(inputStream, outputStream, "Sheet1", 0, 0, schema, converters);
} catch (ExcelSchemaException e) {
    // A specific error is thrown
    System.out.println(e.getMessage());
    // Output:
    // Caused by:
    //   [A4] Cannot match schema [RECORD test [...], "null"]
    //   Caused by:
    //     [A4] Cannot match schema RECORD test [...]
    //     Caused by:
    //       [A4] Failed to match record for field field_num
    //       [B4] Cell type 'STRING' is not NUMERIC
}
```

### 4. Avro Schema Inference

If you don't have a predefined Avro schema, this module can infer one from the structure and data of an Excel file.

**Example:**
```java
import org.fanchuo.avroexcel.infer.ExcelInferSchema;
import org.fanchuo.avroexcel.converters.DefaultConverters;
import org.apache.avro.Schema;
import java.io.InputStream;

// ...

InputStream excelStream = new FileInputStream("data.xlsx");
Schema inferedSchema = ExcelInferSchema.inferSchema(excelStream, "Sheet1", 1, 2, new DefaultConverters().getExcelFieldParser());
```