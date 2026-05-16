# Avro Excel API
## Features

### 1. Avro to Excel Conversion

Easily transform your data from Avro to Excel format. The library handles the creation of a tabular representation of your data, including complex types like nested records, arrays, and unions.

**Example:**
```java
import org.fanchuo.avroexcel.core.avroutil.Conversion;
import org.fanchuo.avroexcel.core.avroutil.DefaultGenericDataConf;
import org.fanchuo.avroexcel.core.decoder.AvroDecoderBuilder;
import org.fanchuo.avroexcel.excel.converters.DefaultConverters;
import org.fanchuo.avroexcel.excel.encoder.ExcelEncoderBuilder;
import java.io.File;
import java.nio.file.Path;

// ...

File avroFile = new File("users.avro");
File excelFile = new File("users.xlsx");

// Assuming you have a DefaultGenericDataConf instance and DefaultConverters instance
new Convertion().convert(
    avroFile.toPath(),
    excelFile.toPath(),
    new AvroDecoderBuilder(new DefaultGenericDataConf()),
    new ExcelEncoderBuilder("Avro Data", 1, 2, new DefaultConverters())
);
```

### 2. Excel to Avro Conversion

Convert an Excel file to an Avro file based on a predefined Avro schema. This is particularly useful for importing manually entered data into an Avro-based system.

**Example:**
```java
import org.fanchuo.avroexcel.core.avroutil.Conversion;
import org.fanchuo.avroexcel.core.avroutil.DefaultGenericDataConf;
import org.fanchuo.avroexcel.core.encoder.AvroEncoderBuilder;
import org.fanchuo.avroexcel.excel.converters.DefaultConverters;
import org.fanchuo.avroexcel.excel.decoder.ExcelDecoderBuilder;
import org.apache.avro.Schema;
import java.io.File;
import java.nio.file.Path;

// ...

File excelFile = new File("users.xlsx");
File avroFile = new File("users.avro");
Schema schema = new Schema.Parser().parse(new File("user.avsc")); // Your Avro schema file

// Assuming you have a DefaultGenericDataConf instance and DefaultConverters instance
new Convertion().convert(
    excelFile.toPath(),
    avroFile.toPath(),
    new ExcelDecoderBuilder(new DefaultConverters(), "Avro Data", schema, 1, 2),
    new AvroEncoderBuilder(new DefaultGenericDataConf())
);
```

### 3. Excel File Validation

The most powerful feature of this module is the validation of an Excel file's content against an Avro schema. If the Excel file does not comply with the schema (incorrect data types, missing or extra fields, etc.), a `DecoderSchemaException` is thrown with detailed information to locate the error.

**Error detection example:**
```java
import org.fanchuo.avroexcel.core.avroutil.Conversion;
import org.fanchuo.avroexcel.core.avroutil.DefaultGenericDataConf;
import org.fanchuo.avroexcel.core.decoder.DecoderSchemaException;
import org.fanchuo.avroexcel.core.encoder.AvroEncoderBuilder;
import org.fanchuo.avroexcel.excel.converters.DefaultConverters;
import org.fanchuo.avroexcel.excel.decoder.ExcelDecoderBuilder;
import org.apache.avro.Schema;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;

// ...

// Assuming 'schema' is your Avro schema and 'excelInputStream' is your Excel file input stream
Schema schema = new Schema.Parser().parse("{\"type\": \"record\", \"name\": \"test\", \"fields\": [{\"name\": \"field_num\", \"type\": \"double\"}]}");
InputStream excelInputStream = new FileInputStream("invalid_data.xlsx"); // An Excel file with invalid data
OutputStream outputStream = new ByteArrayOutputStream(); // Or any other output stream

new Convertion().onErrors(x -> {
    System.out.println(x);
    // Output example (similar to the test case):
    // Caused by:
    //   [A4] Cannot match schema RECORD test [...]
    //   Caused by:
    //     [A4] Failed to match record for field field_num
    //     [B4] Cell type 'STRING' is not NUMERIC
}).convert(
    excelInputStream,
    outputStream,
    new ExcelDecoderBuilder(new DefaultConverters(), "Sheet1", schema, 0, 0),
    new AvroEncoderBuilder(new DefaultGenericDataConf())
);
```

### 4. Avro Schema Inference

If you don't have a predefined Avro schema, this module can infer one from the structure and data of an Excel file.

**Example:**
```java
import org.fanchuo.avroexcel.infer.ExcelInferSchema;
import org.fanchuo.avroexcel.excel.converters.DefaultConverters;
import org.apache.avro.Schema;
import java.io.InputStream;
import java.io.FileInputStream;

// ...

InputStream excelStream = new FileInputStream("data.xlsx");
Schema inferredSchema = ExcelInferSchema.inferSchema(excelStream, "Sheet1", 1, 2, new DefaultConverters().getExcelFieldParser());
System.out.println(inferredSchema.toString(true)); // Print the inferred schema
```

### 5. Avro Logical Types Support

The library provides robust support for various Avro logical types, ensuring accurate data conversion between Avro and Excel formats. This includes handling of complex types like UUIDs, Decimals, Dates, and Durations.

**Examples of Supported Logical Types:**
*   `date` (represented as `int` in Avro, e.g., `java.time.LocalDate`)
*   `time-millis` (represented as `int` in Avro, e.g., `java.time.LocalTime`)
*   `timestamp-millis` (represented as `long` in Avro, e.g., `java.time.Instant`)
*   `local-timestamp-millis` (represented as `long` in Avro, e.g., `java.time.LocalDateTime`)
*   `uuid` (represented as `string` or `fixed` in Avro, e.g., `java.util.UUID`)
*   `decimal` (represented as `bytes` or `fixed` in Avro, e.g., `java.math.BigDecimal`)
*   `duration` (represented as `fixed` in Avro, e.g., `java.time.Duration`)
