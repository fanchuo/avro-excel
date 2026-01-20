package org.fanchuo.avroexcel.cli;

import java.io.File;
import java.util.concurrent.Callable;
import org.fanchuo.avroexcel.AvroToExcelConverter;
import picocli.CommandLine;

@CommandLine.Command(name = "AvroExcel", version = "1.0.0", mixinStandardHelpOptions = true)
public class AvroExcel implements Callable<Void> {

  @CommandLine.Option(
      names = {"-i"},
      description = "Input file")
  private File inputFile;

  @CommandLine.Option(
      names = {"-o"},
      description = "Output file")
  private File outputFile;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new AvroExcel()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Void call() throws Exception {
    new AvroToExcelConverter().convert(inputFile, outputFile, "Onglet", 0, 0);
    return null;
  }
}
