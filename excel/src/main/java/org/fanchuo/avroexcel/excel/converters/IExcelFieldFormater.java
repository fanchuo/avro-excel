package org.fanchuo.avroexcel.excel.converters;

import org.apache.poi.ss.usermodel.Cell;
import org.fanchuo.avroexcel.core.avroutil.Zone;

public interface IExcelFieldFormater {
  void colorExcelField(Cell c, Zone zone);

  void formatExcelField(Cell cell, Object value, Zone zone);
}
