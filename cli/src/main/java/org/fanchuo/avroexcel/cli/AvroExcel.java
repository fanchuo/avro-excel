package org.fanchuo.avroexcel.cli;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import org.apache.avro.Schema;
import org.fanchuo.avroexcel.core.api.CodecService;
import org.fanchuo.avroexcel.core.avroutil.Conversion;
import org.fanchuo.avroexcel.core.avroutil.DefaultGenericDataConf;
import org.fanchuo.avroexcel.core.avroutil.IGenericDataConf;
import org.fanchuo.avroexcel.core.decoder.IDecoderBuilder;
import org.fanchuo.avroexcel.core.encoder.IEncoderBuilder;
import org.fanchuo.avroexcel.excel.converters.DefaultConverters;
import org.fanchuo.avroexcel.excel.converters.IConverters;
import org.fanchuo.avroexcel.excel.decoder.ExcelDecoderBuilder;
import org.fanchuo.avroexcel.excel.encoder.ExcelEncoderBuilder;
import org.fanchuo.avroexcel.excel.infer.ExcelInferSchema;
import picocli.CommandLine;

@CommandLine.Command(name = "avroexcel-cli", version = "1.0.0", mixinStandardHelpOptions = true)
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

  @CommandLine.Option(
      names = {"-e"},
      description = "Input encoding type")
  private String inputEncoding;

  @CommandLine.Option(
      names = {"-f"},
      description = "Output encoding type")
  private String outputEncoding;

  public static void main(String[] args) {
    int exitCode = execute(args);
    System.exit(exitCode);
  }

  public static int execute(String... args) {
    return new CommandLine(new AvroExcel()).execute(args);
  }

  @Override
  public Void call() throws Exception {
    IConverters converters = new DefaultConverters();
    IGenericDataConf genericDataConf = new DefaultGenericDataConf();
    ServiceLoader<CodecService> loader = ServiceLoader.load(CodecService.class);
    Map<String, CodecService> codecs = new HashMap<>();
    for (CodecService codecService : loader) {
      codecs.put(codecService.getName(), codecService);
    }
    IEncoderBuilder encoderBuilder;
    IDecoderBuilder decoderBuilder;
    if ("EXCEL".equals(this.inputEncoding)) {
      Schema schema;
      if (schemaFile != null) {
        schema = new Schema.Parser().parse(schemaFile);
      } else {
        schema =
            ExcelInferSchema.inferSchema(
                inputFile, tab, col, row, converters.getExcelFieldParser());
      }
      decoderBuilder = new ExcelDecoderBuilder(converters, this.tab, schema, this.col, this.row);
    } else {
      CodecService codec = codecs.get(this.inputEncoding);
      decoderBuilder = codec.makeDecoder(genericDataConf);
    }
    if ("EXCEL".equals(this.outputEncoding)) {
      encoderBuilder = new ExcelEncoderBuilder(this.tab, this.col, this.row, converters);
    } else {
      CodecService codec = codecs.get(this.outputEncoding);
      encoderBuilder = codec.makeEncoder(genericDataConf);
    }
    new Conversion()
        .convert(this.inputFile.toPath(), this.outputFile.toPath(), decoderBuilder, encoderBuilder);
    return null;
  }
}
