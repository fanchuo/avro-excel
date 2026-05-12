package org.fanchuo.avroexcel.core.avroutil;

import org.apache.avro.generic.GenericRecord;

@FunctionalInterface
public interface ValidationHandler {
  ErrorMessage validate(Object address, GenericRecord genericRecord);
}
