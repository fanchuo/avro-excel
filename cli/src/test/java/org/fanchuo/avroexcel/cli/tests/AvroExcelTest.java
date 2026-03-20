package org.fanchuo.avroexcel.cli.tests;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import org.fanchuo.avroexcel.cli.AvroExcel;
import org.fanchuo.avroexcel.testfiles.ExcelWorkbookDescriptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    AvroExcel.execute(
        "-i", inputExcel, "-o", outputParquet, "-t", "Test1", "-e", "EXCEL", "-f", "PARQUET");
    AvroExcel.execute("-i", outputParquet, "-o", outputExcel, "-e", "PARQUET", "-f", "EXCEL");
    List<String> content = ExcelWorkbookDescriptor.dump(new File(outputExcel), "tab");
    List<String> expectedContent = new ArrayList<>();
    try (InputStream is = ClassLoader.getSystemResourceAsStream("expected_excel.txt")) {
      if (is != null) {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line;
        while ((line = br.readLine()) != null) {
          expectedContent.add(line);
        }
      }
    }
    Assertions.assertLinesMatch(expectedContent, content);
  }
}
