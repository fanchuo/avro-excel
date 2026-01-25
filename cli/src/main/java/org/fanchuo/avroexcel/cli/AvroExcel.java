package org.fanchuo.avroexcel.cli;

import java.io.File;
import java.util.concurrent.Callable;
import org.apache.avro.Schema;
import org.fanchuo.avroexcel.ExcelToAvroConverter;
import picocli.CommandLine;

@CommandLine.Command(name = "AvroExcel", version = "1.0.0", mixinStandardHelpOptions = true)
public class AvroExcel implements Callable<Void> {

  @CommandLine.Option(
      names = {"-i"},
      description = "Input file",
      required = true)
  private File inputFile;

  @CommandLine.Option(
      names = {"-o"},
      description = "Output file",
      required = true)
  private File outputFile;

  @CommandLine.Option(
      names = {"-s"},
      description = "Schema file")
  private File schemaFile;

  @CommandLine.Option(
      names = {"-c"},
      description = "Origin column in Excel",
      defaultValue = "0")
  private int col;

  @CommandLine.Option(
      names = {"-r"},
      description = "Origin row in Excel",
      defaultValue = "0")
  private int row;

  @CommandLine.Option(
      names = {"-t"},
      description = "Origin tab in Excel")
  private String tab;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new AvroExcel()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Void call() throws Exception {
    Schema schema = new Schema.Parser().parse(schemaFile);
    ExcelToAvroConverter.convert(inputFile, outputFile, tab, col, row, schema);
    return null;
  }
}
