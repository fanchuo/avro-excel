package org.fanchuo.avroexcel;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AvroToExcelConverterTest {

    private static final Path TEST_OUTPUT_DIR = Path.of("build", "test-output");

    @BeforeEach
    void setUp() throws IOException {
        if (Files.exists(TEST_OUTPUT_DIR)) {
            // Recursively delete the directory
            Files.walk(TEST_OUTPUT_DIR)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        Files.createDirectories(TEST_OUTPUT_DIR);
    }

    @Test
    void convert() throws IOException {
        File avroFile = TEST_OUTPUT_DIR.resolve("users.avro").toFile();
        createSampleAvroFile(avroFile);

        File excelFile = TEST_OUTPUT_DIR.resolve("users.xlsx").toFile();

        AvroToExcelConverter converter = new AvroToExcelConverter();
        converter.convert(avroFile, excelFile);

        assertTrue(excelFile.exists());
        assertTrue(excelFile.length() > 0);
    }

    private void createSampleAvroFile(File file) throws IOException {
        Schema schema = new Schema.Parser().parse(getClass().getResourceAsStream("/user.avsc"));
        Schema favoriteSchema = schema.getField("favorite").schema();
        Integer pointSchemaIdx = schema.getField("lst2").schema().getIndexNamed("array");
        Schema pointSchema = schema.getField("lst2").schema().getTypes().get(pointSchemaIdx).getElementType();

        DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(schema);
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