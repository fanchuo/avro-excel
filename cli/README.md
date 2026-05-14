# Avro Excel CLI

## Install

```bash
# Your java runtime environment should already be setup.
$ java -version
openjdk version "21.0.10" 2026-01-20

# Unzip the Archive
$ unzip avroexcel-cli.zip
$ cd avroexcel-cli/
```

## Basic usage
```bash
$ ./bin/avroexcel-cli --help
Usage: avroexcel-cli [-hV] [-c=<col>] [-e=<inputEncoding>]
                     [-f=<outputEncoding>] -i=<inputFile> -o=<outputFile>
                     [-r=<row>] [-s=<schemaFile>] [-t=<tab>]
  -c=<col>               Origin column in Excel
  -e=<inputEncoding>     Input encoding type
  -f=<outputEncoding>    Output encoding type
  -h, --help             Show this help message and exit.
  -i=<inputFile>         Input file
  -o=<outputFile>        Output file
  -r=<row>               Origin row in Excel
  -s=<schemaFile>        Schema file
  -t=<tab>               Origin tab in Excel
  -V, --version          Print version information and exit.
```

Beyond this, consider adding <AVROEXCEL_PATH>/bin to your PATH environment.

## Examples

```bash
# Convert Excel to Avro
avroexcel-cli -i input.xlsx -o output.avro -t Sheet1 -e EXCEL -f AVRO --schema user.avsc

# Convert Avro to Excel
avroexcel-cli -i input.avro -o output.xlsx -t Sheet1 -e AVRO -f EXCEL

# Infer schema from Excel and convert to Avro
avroexcel-cli -i input.xlsx -o output.avro -t Sheet1 -e EXCEL -f AVRO --infer-schema
```
