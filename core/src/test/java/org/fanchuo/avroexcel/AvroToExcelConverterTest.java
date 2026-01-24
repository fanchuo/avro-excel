package org.fanchuo.avroexcel;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.commons.io.IOUtils;
import org.fanchuo.avroexcel.encoder.ExcelSchemaException;
import org.fanchuo.avroexcel.infer.ExcelInferSchema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AvroToExcelConverterTest {

  private static final Path TEST_OUTPUT_DIR = Path.of("build", "test-output");

  @BeforeEach
  void setUp() throws IOException {
    if (Files.exists(TEST_OUTPUT_DIR)) {
      // Recursively delete the directory
      try (Stream<Path> paths = Files.walk(TEST_OUTPUT_DIR)) {
        boolean result =
            paths.sorted(Comparator.reverseOrder()).map(Path::toFile).allMatch(File::delete);
        if (!result) throw new IOException("Failed to delete directory");
      }
    }
    Files.createDirectories(TEST_OUTPUT_DIR);
  }

  @Test
  void convert() throws Exception {
    Schema schema = new Schema.Parser().parse(getClass().getResourceAsStream("/user.avsc"));
    File avroFile = TEST_OUTPUT_DIR.resolve("users.avro").toFile();
    createSampleAvroFile(avroFile, schema);

    File excelFile = TEST_OUTPUT_DIR.resolve("users.xlsx").toFile();

    AvroToExcelConverter converter = new AvroToExcelConverter();
    converter.convert(avroFile, excelFile, "Avro Data", 1, 2);

    assertTrue(excelFile.exists());
    assertTrue(excelFile.length() > 0);

    List<String> dump = ExcelWorkbookDescriptor.dump(excelFile, "Avro Data");
    System.out.println(String.join("\n", dump));
    StringWriter sw = new StringWriter();
    URL url = getClass().getResource("/excel_awaited_dump.txt");
    assertNotNull(url);
    try (InputStream is = url.openStream();
        Reader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
      IOUtils.copy(r, sw);
    }
    Assertions.assertLinesMatch(Arrays.asList(sw.toString().split("\n")), dump);
    File backAvroFile = TEST_OUTPUT_DIR.resolve("back_users.avro").toFile();
    ExcelToAvroConverter.convert(excelFile, backAvroFile, "Avro Data", 1, 2, schema);
    List<String> dump2 = AvroDescriptor.convert(backAvroFile);
    System.out.println(String.join("\n", dump2));
    StringWriter sw2 = new StringWriter();
    URL url2 = getClass().getResource("/reencoded.jsons");
    assertNotNull(url2);
    try (InputStream is2 = url2.openStream();
        Reader r2 = new InputStreamReader(is2, StandardCharsets.UTF_8)) {
      IOUtils.copy(r2, sw2);
    }
    Assertions.assertLinesMatch(Arrays.asList(sw2.toString().split("\n")), dump2);
    Schema inferedSchema;
    try (InputStream is = new FileInputStream(excelFile)) {
      inferedSchema = ExcelInferSchema.inferSchema(is, "Avro Data", 1, 2);
    }
    File temp = File.createTempFile("test", ".avro");
    temp.deleteOnExit();
    ExcelToAvroConverter.convert(excelFile, temp, "Avro Data", 1, 2, inferedSchema);
  }

  @Test
  public void validate() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Schema date = new Schema.Parser().parse("{\"type\": \"int\", \"logicalType\": \"date\"}");
    Schema datetime =
        new Schema.Parser()
            .parse("{\"type\": \"long\", \"logicalType\": \"local-timestamp-millis\"}");
    Schema schema =
        Schema.createRecord(
            "test",
            null,
            null,
            false,
            Arrays.asList(
                new Schema.Field("field_txt", Schema.create(Schema.Type.STRING)),
                new Schema.Field("field_num", Schema.create(Schema.Type.DOUBLE)),
                new Schema.Field("field_bool", Schema.create(Schema.Type.BOOLEAN)),
                new Schema.Field("field_date", date),
                new Schema.Field("field_time", datetime)));
    try (InputStream is = getClass().getResourceAsStream("/tests.xlsx")) {
      ExcelToAvroConverter.convert(is, baos, "Test1", 0, 0, schema);
      fail("Should not have failed");
    } catch (ExcelSchemaException e) {
      assertEquals(
          "Caused by:\n"
              + "  [A3] Cannot match schema [RECORD test [field_txt, field_num, field_bool, field_date, field_time], \"null\"]\n"
              + "  Caused by:\n"
              + "    [A3] Cannot match schema RECORD test [field_txt, field_num, field_bool, field_date, field_time]\n"
              + "    Caused by:\n"
              + "      [A3] Failed to match schema \"string\"\n"
              + "      [A3] Cell type 'NUMERIC' is not STRING",
          e.getMessage());
    }
  }

  @Test
  public void validate2() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Schema schema =
        Schema.createRecord(
            "test",
            null,
            null,
            false,
            Arrays.asList(
                new Schema.Field("field_txt", Schema.create(Schema.Type.STRING)),
                new Schema.Field("field_num", Schema.create(Schema.Type.DOUBLE)),
                new Schema.Field("field_bool", Schema.create(Schema.Type.BOOLEAN))));
    try (InputStream is = getClass().getResourceAsStream("/tests.xlsx")) {
      ExcelToAvroConverter.convert(is, baos, "Test1", 0, 0, schema);
      fail("Should not have failed");
    } catch (ExcelSchemaException e) {
      assertEquals(
          "Caused by:\n"
              + "  [A2] Cannot match schema [RECORD test [field_txt, field_num, field_bool], \"null\"]\n"
              + "  Caused by:\n"
              + "    [A2] Cannot match schema RECORD test [field_txt, field_num, field_bool]\n"
              + "    [A2] Failed to match schema RECORD test [field_txt, field_num, field_bool], because of additional fields defined [field_date, field_time]",
          e.getMessage());
    }
  }

  @Test
  public void validate3() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Schema recordC =
        Schema.createRecord(
            "testC",
            null,
            null,
            false,
            Collections.singletonList(new Schema.Field("c", Schema.create(Schema.Type.STRING))));
    Schema array = Schema.createArray(Schema.create(Schema.Type.INT));
    Schema unionB = Schema.createUnion(recordC, array);
    Schema schema =
        Schema.createRecord(
            "test",
            null,
            null,
            false,
            Arrays.asList(
                new Schema.Field("a", Schema.create(Schema.Type.STRING)),
                new Schema.Field("b", unionB)));
    try (InputStream is = getClass().getResourceAsStream("/tests.xlsx")) {
      ExcelToAvroConverter.convert(is, baos, "Test2", 0, 0, schema);
      fail("Should not have failed");
    } catch (ExcelSchemaException e) {
      assertEquals(
          "Caused by:\n"
              + "  [A3] Cannot match schema [RECORD test [a, b], \"null\"]\n"
              + "  Caused by:\n"
              + "    [A3] Cannot match schema RECORD test [a, b]\n"
              + "    Caused by:\n"
              + "      [A3] Failed to match schema [RECORD testC [c], ARRAY \"int\"]\n"
              + "      [B3] Cannot be both ARRAY and RECORD",
          e.getMessage());
    }
  }

  @Test
  public void validate4() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Schema enumA = Schema.createEnum("enumA", null, null, Arrays.asList("c", "d"));
    Schema schema =
        Schema.createRecord(
            "test", null, null, false, Collections.singletonList(new Schema.Field("a", enumA)));
    try (InputStream is = getClass().getResourceAsStream("/tests.xlsx")) {
      ExcelToAvroConverter.convert(is, baos, "Test3", 0, 0, schema);
      fail("Should not have failed");
    } catch (ExcelSchemaException e) {
      assertEquals(
          "Caused by:\n"
              + "  [A2] Cannot match schema [RECORD test [a], \"null\"]\n"
              + "  Caused by:\n"
              + "    [A2] Cannot match schema RECORD test [a]\n"
              + "    [A2] Failed to match schema RECORD test [a], because of additional fields defined [b]",
          e.getMessage());
    }
  }

  @Test
  public void validate5() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Schema enumA = Schema.createEnum("enumA", null, null, Arrays.asList("c", "d"));
    Schema schema =
        Schema.createRecord(
            "test",
            null,
            null,
            false,
            Arrays.asList(
                new Schema.Field("a", enumA),
                new Schema.Field("b", Schema.createUnion(enumA, Schema.create(Schema.Type.NULL)))));
    try (InputStream is = getClass().getResourceAsStream("/tests.xlsx")) {
      ExcelToAvroConverter.convert(is, baos, "Test3", 0, 0, schema);
      fail("Should not have failed");
    } catch (ExcelSchemaException e) {
      assertEquals(
          "Caused by:\n"
              + "  [A3] Cannot match schema [RECORD test [a, b], \"null\"]\n"
              + "  Caused by:\n"
              + "    [A3] Cannot match schema RECORD test [a, b]\n"
              + "    Caused by:\n"
              + "      [A3] Failed to match schema [{\"type\":\"enum\",\"name\":\"enumA\",\"symbols\":[\"c\",\"d\"]}, \"null\"]\n"
              + "      [B3] 'e' is not one of [c, d]",
          e.getMessage());
    }
  }

  @Test
  public void validate6() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Schema enumA = Schema.createEnum("enumA", null, null, Arrays.asList("c", "d"));
    Schema schema =
        Schema.createRecord(
            "test",
            null,
            null,
            false,
            Arrays.asList(
                new Schema.Field("a", enumA),
                new Schema.Field(
                    "b",
                    Schema.createUnion(
                        Schema.create(Schema.Type.STRING), Schema.create(Schema.Type.NULL))),
                new Schema.Field(
                    "c",
                    Schema.createUnion(
                        Schema.create(Schema.Type.STRING), Schema.create(Schema.Type.NULL)))));
    try (InputStream is = getClass().getResourceAsStream("/tests.xlsx")) {
      ExcelToAvroConverter.convert(is, baos, "Test3", 0, 0, schema);
    }
  }

  @Test
  public void validate7() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Schema enumA = Schema.createEnum("enumA", null, null, Arrays.asList("c", "d"));
    Schema schema =
        Schema.createRecord(
            "test",
            null,
            null,
            false,
            Arrays.asList(
                new Schema.Field("a", enumA),
                new Schema.Field(
                    "b",
                    Schema.createUnion(
                        Schema.create(Schema.Type.STRING), Schema.create(Schema.Type.NULL))),
                new Schema.Field("c", Schema.create(Schema.Type.STRING))));
    try (InputStream is = getClass().getResourceAsStream("/tests.xlsx")) {
      ExcelToAvroConverter.convert(is, baos, "Test3", 0, 0, schema);
      fail("Should not have failed");
    } catch (ExcelSchemaException e) {
      assertEquals(
          "Caused by:\n"
              + "  [A2] Cannot match schema [RECORD test [a, b, c], \"null\"]\n"
              + "  Caused by:\n"
              + "    [A2] Cannot match schema RECORD test [a, b, c]\n"
              + "    [A2] Failed to find field c for schema RECORD test [a, b, c]",
          e.getMessage());
    }
  }

  private void createSampleAvroFile(File file, Schema schema) throws IOException {
    GenericData genericData = AvroReader.makeGenericData();
    Schema favoriteSchema = schema.getField("favorite").schema();
    Integer pointSchemaIdx = schema.getField("lst2").schema().getIndexNamed("array");
    Schema pointSchema =
        schema.getField("lst2").schema().getTypes().get(pointSchemaIdx).getElementType();
    Integer weirdIdx = schema.getField("strange_stuff").schema().getIndexNamed("array");
    Schema weird =
        schema.getField("strange_stuff").schema().getTypes().get(weirdIdx).getElementType();
    Schema weird2 = weird.getField("strange_map").schema().getValueType();

    DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(schema, genericData);
    try (DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<>(datumWriter)) {
      dataFileWriter.create(schema, file);

      // User 1
      GenericRecord user1 = new GenericData.Record(schema);
      GenericRecord favorite1 = new GenericData.Record(favoriteSchema);
      favorite1.put("number", 256);
      favorite1.put("color", null);
      user1.put("name", "Alyssa");
      user1.put("favorite", favorite1);
      user1.put("creation_date", Instant.parse("2025-01-01T00:00:00.000Z").toEpochMilli());
      user1.put("liste_optionnelle", Collections.EMPTY_LIST);
      user1.put("lst2", null);
      user1.put("lst3", "ABC");
      Map<String, String> mapExample = new HashMap<>();
      mapExample.put("a", "b");
      mapExample.put("c", "d");
      user1.put("map_example", mapExample);
      List<GenericRecord> strangeStuff = new ArrayList<>();
      GenericRecord strangeStuff1 = new GenericData.Record(weird);
      strangeStuff.add(strangeStuff1);
      strangeStuff1.put("bool_val", Boolean.TRUE);
      strangeStuff1.put("local_date", LocalDate.of(2025, 1, 2));
      strangeStuff1.put("local_datetime", LocalDateTime.of(2025, 1, 25, 19, 45));
      GenericRecord strangeStuff2 = new GenericData.Record(weird);
      strangeStuff.add(strangeStuff2);
      strangeStuff2.put("bool_val", Boolean.FALSE);
      strangeStuff2.put("local_date", LocalDate.of(2025, 2, 1));
      strangeStuff2.put("local_datetime", LocalDateTime.of(2025, 3, 25, 19, 45));
      Map<String, GenericRecord> strangeMap1 = new HashMap<>();
      strangeStuff1.put("strange_map", strangeMap1);
      Map<String, GenericRecord> strangeMap2 = new HashMap<>();
      strangeStuff2.put("strange_map", strangeMap2);
      GenericRecord weird11 = new GenericData.Record(weird2);
      weird11.put("a", "z");
      weird11.put("b", "y");
      strangeMap1.put("comp", weird11);
      GenericRecord weird12 = new GenericData.Record(weird2);
      weird12.put("a", "Z");
      weird12.put("b", "Y");
      strangeMap1.put("COMP2", weird12);
      GenericRecord weird21 = new GenericData.Record(weird2);
      weird21.put("a", "123");
      weird21.put("b", "456");
      strangeMap2.put("comp", weird21);
      GenericRecord weird22 = new GenericData.Record(weird2);
      weird22.put("a", "098");
      weird22.put("b", "765");
      strangeMap2.put("qwerty", weird22);
      user1.put("strange_stuff", strangeStuff);
      dataFileWriter.append(user1);

      // User 2
      GenericRecord user2 = new GenericData.Record(schema);
      GenericRecord favorite2 = new GenericData.Record(favoriteSchema);
      favorite2.put("number", 7);
      favorite2.put("color", "red");
      user2.put("name", "Ben");
      user2.put("favorite", favorite2);
      user2.put("creation_date", Instant.parse("2025-01-02T00:00:00.000Z").toEpochMilli());
      user2.put("liste_optionnelle", Collections.singleton(123.4));
      GenericRecord point1 = new GenericData.Record(pointSchema);
      point1.put("x", "azerty");
      point1.put("y", "uiop");
      GenericRecord point2 = new GenericData.Record(pointSchema);
      point2.put("x", "qsdfg");
      point2.put("y", "hjklm");
      user2.put("lst2", Arrays.asList(point1, point2));
      user2.put("lst3", "DEF");
      user2.put("matrix", Arrays.asList(Arrays.asList(1.0, 2.0), Arrays.asList(3.0, 4.0)));
      dataFileWriter.append(user2);

      // User 3
      GenericRecord user3 = new GenericData.Record(schema);
      GenericRecord favorite3 = new GenericData.Record(favoriteSchema);
      favorite3.put("number", null);
      favorite3.put("color", "blue");
      user3.put("name", "Charlie");
      user3.put("favorite", favorite3);
      user3.put("creation_date", Instant.parse("2025-01-03T00:00:00.000Z").toEpochMilli());
      user3.put("liste_optionnelle", Collections.singleton(null));
      GenericRecord point = new GenericData.Record(pointSchema);
      point.put("x", "xx");
      point.put("y", "yy");
      user3.put("lst2", Collections.singleton(point));
      GenericRecord pointToo = new GenericData.Record(pointSchema);
      pointToo.put("x", "GHI");
      pointToo.put("y", "JKL");
      user3.put("lst3", pointToo);
      dataFileWriter.append(user3);
    }
  }
}
