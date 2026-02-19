package org.fanchuo.avroexcel.converters;

import org.apache.avro.Schema;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.util.CellAddress;
import org.fanchuo.avroexcel.excelutil.ErrorMessage;

public interface IExcelFieldParser {
  abstract class TypeParser {
    public ErrorMessage errorMessage;
    public Object value;

    public abstract void analyze(Schema schema, Cell cell, CellAddress address);

    public boolean isCompatible() {
      return errorMessage == null;
    }
  }

  TypeParser checkCompatible(Schema s, Cell cell, CellAddress address);
}
