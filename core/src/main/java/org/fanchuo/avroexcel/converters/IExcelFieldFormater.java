package org.fanchuo.avroexcel.converters;

import org.apache.poi.ss.usermodel.Cell;

public interface IExcelFieldFormater {
  void colorExcelField(Cell c, Zone zone);

  void formatExcelField(Cell cell, Object value, Zone zone);
}
