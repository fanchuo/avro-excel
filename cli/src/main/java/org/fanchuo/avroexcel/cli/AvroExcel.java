package org.fanchuo.avroexcel.cli;

import java.io.File;
import java.util.concurrent.Callable;
import org.apache.avro.Schema;
import org.fanchuo.avroexcel.core.avroutil.Convertion;
import org.fanchuo.avroexcel.core.avroutil.DefaultGenericDataConf;
import org.fanchuo.avroexcel.core.avroutil.IGenericDataConf;
import org.fanchuo.avroexcel.core.decoder.AvroDecoderBuilder;
import org.fanchuo.avroexcel.core.decoder.IDecoderBuilder;
import org.fanchuo.avroexcel.core.encoder.AvroEncoderBuilder;
import org.fanchuo.avroexcel.core.encoder.IEncoderBuilder;
import org.fanchuo.avroexcel.excel.converters.DefaultConverters;
import org.fanchuo.avroexcel.excel.converters.IConverters;
import org.fanchuo.avroexcel.excel.decoder.ExcelDecoderBuilder;
import org.fanchuo.avroexcel.excel.encoder.ExcelEncoderBuilder;
import org.fanchuo.avroexcel.excel.infer.ExcelInferSchema;
import org.fanchuo.avroexcel.parquet.decoder.ParquetDecoderBuilder;
import org.fanchuo.avroexcel.parquet.encoder.ParquetEncoderBuilder;
import picocli.CommandLine;

@CommandLine.Command(name = "AvroExcel", version = "1.0.0", mixinStandardHelpOptions = true)
public class AvroExcel implements Callable<Void> {

  private enum Encoding {
    EXCEL,
    AVRO,
    PARQUET,
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
      description = "Input encoding type")
  private Encoding inputEncoding;

  @CommandLine.Option(
      names = {"-f"},
      description = "Output encoding type")
  private Encoding outputEncoding;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new AvroExcel()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Void call() throws Exception {
    IConverters converters = new DefaultConverters();
    IGenericDataConf genericDataConf = new DefaultGenericDataConf();
    IEncoderBuilder encoderBuilder;
    IDecoderBuilder decoderBuilder;
    switch (this.inputEncoding) {
      case EXCEL:
        Schema schema;
        if (schemaFile != null) {
          schema = new Schema.Parser().parse(schemaFile);
        } else {
          schema =
              ExcelInferSchema.inferSchema(
                  inputFile, tab, col, row, converters.getExcelFieldParser());
        }
        decoderBuilder = new ExcelDecoderBuilder(converters, this.tab, schema, this.col, this.row);
        break;
      case PARQUET:
        decoderBuilder = new ParquetDecoderBuilder(genericDataConf);
        break;
      case AVRO:
      default:
        decoderBuilder = new AvroDecoderBuilder(genericDataConf);
        break;
    }
    switch (this.outputEncoding) {
      case EXCEL:
        encoderBuilder = new ExcelEncoderBuilder(this.tab, this.col, this.row, converters);
        break;
      case PARQUET:
        encoderBuilder = new ParquetEncoderBuilder(genericDataConf);
        break;
      case AVRO:
      default:
        encoderBuilder = new AvroEncoderBuilder(genericDataConf);
    }
    Convertion.convert(
        this.inputFile.toPath(), this.outputFile.toPath(), decoderBuilder, encoderBuilder);
    return null;
  }
}
