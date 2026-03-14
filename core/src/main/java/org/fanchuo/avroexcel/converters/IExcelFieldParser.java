package org.fanchuo.avroexcel.converters;

import org.apache.avro.Schema;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.util.CellAddress;
import org.fanchuo.avroexcel.avroutil.ParserResult;
import org.fanchuo.avroexcel.avroutil.Type;
import org.fanchuo.avroexcel.infer.InferSchemaException;

public interface IExcelFieldParser {
  ParserResult checkCompatible(Schema s, Cell cell, CellAddress address);

  Type guessType(Cell cell, CellAddress address) throws InferSchemaException;
}
