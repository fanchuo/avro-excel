package org.fanchuo.avroexcel.converters;

import org.apache.avro.Schema;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.util.CellAddress;

public interface IExcelFieldParser {
  ParserResult checkCompatible(Schema s, Cell cell, CellAddress address);

  Type guessType(Cell cell, CellAddress address) throws InferSchemaException;
}
