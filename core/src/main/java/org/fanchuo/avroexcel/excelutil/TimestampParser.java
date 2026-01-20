package org.fanchuo.avroexcel.excelutil;

import java.text.ParsePosition;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import org.apache.poi.ss.usermodel.Cell;

public class TimestampParser {
  private TimestampParser() {}

  public static Instant parseDate(Cell cell) {
    String str = cell.getStringCellValue();
    ParsePosition position = new ParsePosition(0);
    TemporalAccessor temporalAccessor =
        DateTimeFormatter.ISO_INSTANT.parseUnresolved(str, position);
    if (position.getErrorIndex() < 0) {
      return Instant.from(temporalAccessor);
    }
    return null;
  }
}
