package org.fanchuo.avroexcel.cli;

import java.io.File;
import java.util.concurrent.Callable;
import org.apache.avro.Schema;
import org.fanchuo.avroexcel.converters.DefaultConverters;
import org.fanchuo.avroexcel.converters.DefaultGenericDataConf;
import org.fanchuo.avroexcel.converters.IConverters;
import org.fanchuo.avroexcel.converters.IGenericDataConf;
import org.fanchuo.avroexcel.decoder.AvroToExcelConverter;
import org.fanchuo.avroexcel.encoder.ExcelToAvroConverter;
import org.fanchuo.avroexcel.infer.ExcelInferSchema;
import picocli.CommandLine;

@CommandLine.Command(name = "AvroExcel", version = "1.0.0", mixinStandardHelpOptions = true)
public class AvroExcel implements Callable<Void> {

  private enum Encoding {
    EXCEL_TO_AVRO,
    AVRO_TO_EXCEL
  }

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

  @CommandLine.Option(
      names = {"-e"},
      description = "Encoding type")
  private Encoding encoding;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new AvroExcel()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Void call() throws Exception {
    IConverters converters = new DefaultConverters();
    IGenericDataConf genericDataConf = new DefaultGenericDataConf();
    switch (encoding) {
      case EXCEL_TO_AVRO:
        Schema schema;
        if (schemaFile != null) {
          schema = new Schema.Parser().parse(schemaFile);
        } else {
          schema =
              ExcelInferSchema.inferSchema(
                  inputFile, tab, col, row, converters.getExcelFieldParser());
        }
        ExcelToAvroConverter.convert(
            inputFile, outputFile, tab, col, row, schema, converters, genericDataConf);
        break;
      case AVRO_TO_EXCEL:
        AvroToExcelConverter.convert(
            inputFile, outputFile, tab, col, row, converters, genericDataConf);
        break;
    }
    return null;
  }
}
