package org.fanchuo.avroexcel.converters;

import org.apache.poi.ss.usermodel.Cell;
import org.fanchuo.avroexcel.excelutil.Zone;

public interface IExcelFieldFormater {
  void colorExcelField(Cell c, Zone zone);

  void formatExcelField(Cell cell, Object value, Zone zone);
}
