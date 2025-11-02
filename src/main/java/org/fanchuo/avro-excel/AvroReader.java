package org.example;

import org.apache.avro.Schema;
import org.apache.avro.data.TimeConversions;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public class AvroReader implements Closeable {
    public static GenericData makeGenericData() {
        GenericData genericData = GenericData.get();
        genericData.addLogicalTypeConversion(new TimeConversions.DateConversion());
        genericData.addLogicalTypeConversion(new TimeConversions.TimestampMillisConversion());
        genericData.addLogicalTypeConversion(new TimeConversions.TimeMicrosConversion());
        genericData.addLogicalTypeConversion(new TimeConversions.TimeMillisConversion());
        genericData.addLogicalTypeConversion(new TimeConversions.TimestampMicrosConversion());
        genericData.addLogicalTypeConversion(new TimeConversions.LocalTimestampMicrosConversion());
        genericData.addLogicalTypeConversion(new TimeConversions.LocalTimestampNanosConversion());
        genericData.addLogicalTypeConversion(new TimeConversions.LocalTimestampMillisConversion());
        return genericData;
    }

    private final Iterable<GenericRecord> iterable;
    private final Closeable closeable;
    private final Schema schema;

    public AvroReader(File avroFile) throws IOException {
        DatumReader<GenericRecord> datumReader = new GenericDatumReader<>(null, null, makeGenericData());
        DataFileReader<GenericRecord> dataFileReader = new DataFileReader<>(avroFile, datumReader);
        this.iterable = dataFileReader;
        this.closeable = dataFileReader;
        this.schema = dataFileReader.getSchema();
    }

    public AvroReader(Iterable<GenericRecord> iterable, Closeable closeable, Schema schema) {
        this.iterable = iterable;
        this.closeable = closeable;
        this.schema = schema;
    }

    public void process(Consumer<GenericRecord> consumer) {
        for (GenericRecord record : this.iterable) {
            consumer.accept(record);
        }
    }

    @Override
    public void close() throws IOException {
        if (this.closeable != null) this.closeable.close();
    }

    public Schema getSchema() {
        return schema;
    }
}
