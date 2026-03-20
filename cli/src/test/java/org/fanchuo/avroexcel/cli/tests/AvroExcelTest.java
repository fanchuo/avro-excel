package org.fanchuo.avroexcel.cli.tests;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import org.fanchuo.avroexcel.cli.AvroExcel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class AvroExcelTest {

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
    String inputExcel = ClassLoader.getSystemResource("tests.xlsx").getPath();
    String outputParquet = new File(TEST_OUTPUT_DIR.toFile(), "output.parquet").getPath();
    String outputExcel = new File(TEST_OUTPUT_DIR.toFile(), "output.xlsx").getPath();
    new CommandLine(new AvroExcel())
        .execute(
            "-i", inputExcel, "-o", outputParquet, "-t", "Test1", "-e", "EXCEL", "-f", "PARQUET");
    new CommandLine(new AvroExcel())
        .execute("-i", outputParquet, "-o", outputExcel, "-e", "PARQUET", "-f", "EXCEL");
  }
}
