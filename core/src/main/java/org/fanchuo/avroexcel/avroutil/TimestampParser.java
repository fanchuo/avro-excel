package org.fanchuo.avroexcel.avroutil;

import java.text.ParsePosition;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public class TimestampParser {
  private TimestampParser() {}

  public static Instant parseDate(String str) {
    ParsePosition position = new ParsePosition(0);
    TemporalAccessor temporalAccessor =
        DateTimeFormatter.ISO_INSTANT.parseUnresolved(str, position);
    if (position.getErrorIndex() < 0) {
      return Instant.from(temporalAccessor);
    }
    return null;
  }
}
