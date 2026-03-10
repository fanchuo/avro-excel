package org.fanchuo.avroexcel.converters;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import org.apache.avro.generic.GenericFixed;
import org.apache.poi.ss.usermodel.*;
import org.fanchuo.avroexcel.excelutil.BytesUtils;

public class ExcelFieldFormater implements IExcelFieldFormater {

  private final EnumMap<Zone, CellStyle> regularStyle = new EnumMap<>(Zone.class);
  private final EnumMap<Zone, CellStyle> dateStyle = new EnumMap<>(Zone.class);
  private final EnumMap<Zone, CellStyle> datetimeStyle = new EnumMap<>(Zone.class);

  private static CellStyle makeColor(IndexedColors indexedColor, Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setVerticalAlignment(VerticalAlignment.TOP);
    style.setFillForegroundColor(indexedColor.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    return style;
  }

  @Override
  public void formatExcelField(Cell c, Object value, Zone zone) {
    if (value instanceof Number) {
      c.setCellValue(((Number) value).doubleValue());
    } else if (value instanceof Boolean) {
      c.setCellValue((Boolean) value);
    } else if (value instanceof ByteBuffer) {
      ByteBuffer bb = (ByteBuffer) value;
      c.setCellValue(BytesUtils.bytesToString(bb));
    } else if (value instanceof GenericFixed) {
      GenericFixed f = (GenericFixed) value;
      c.setCellValue(BytesUtils.bytesToString(f.bytes()));
    } else if (value instanceof LocalDate) {
      c.setCellValue((LocalDate) value);
      c.setCellStyle(this.makeDateStyle(zone, c));
    } else if (value instanceof LocalDateTime) {
      c.setCellValue((LocalDateTime) value);
      c.setCellStyle(this.makeDatetimeStyle(zone, c));
    } else {
      c.setCellValue(String.valueOf(value));
    }
  }

  private CellStyle makeDatetimeStyle(Zone zone, Cell c) {
    return this.datetimeStyle.computeIfAbsent(
        zone,
        (k) -> {
          CellStyle style = this.makeRegularStyle(zone, c);
          Workbook workbook = c.getSheet().getWorkbook();
          CellStyle s = workbook.createCellStyle();
          s.cloneStyleFrom(style);
          s.setDataFormat((short) 22);
          return s;
        });
  }

  private CellStyle makeDateStyle(Zone zone, Cell c) {
    return this.dateStyle.computeIfAbsent(
        zone,
        (k) -> {
          CellStyle style = this.makeRegularStyle(zone, c);
          Workbook workbook = c.getSheet().getWorkbook();
          CellStyle s = workbook.createCellStyle();
          s.cloneStyleFrom(style);
          s.setDataFormat((short) 14);
          return s;
        });
  }

  @Override
  public void colorExcelField(Cell c, Zone zone) {
    c.setCellStyle(makeRegularStyle(zone, c));
  }

  private CellStyle makeRegularStyle(Zone zone, Cell cell) {
    return this.regularStyle.computeIfAbsent(
        zone,
        (k) -> {
          Workbook workbook = cell.getSheet().getWorkbook();
          switch (zone) {
            case HEADER:
              return makeColor(IndexedColors.LIGHT_YELLOW, workbook);
            case EVEN:
              return makeColor(IndexedColors.GREY_25_PERCENT, workbook);
          }
          return makeColor(IndexedColors.WHITE, workbook);
        });
  }
}
