package org.fanchuo.avroexcel;

import org.apache.avro.file.DataFileReader;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AvroDescriptor {
    public static List<String> convert(File avroFile) throws IOException {
        GenericData genericData = AvroReader.makeGenericData();
        DatumReader<GenericRecord> datumReader = new GenericDatumReader<>(null, null, genericData);
        List<String> output = new ArrayList<>();
        try (DataFileReader<GenericRecord> dataFileReader = new DataFileReader<>(avroFile, datumReader)) {
            while (dataFileReader.hasNext()) {
                GenericRecord record = dataFileReader.next();
                output.add(record.toString());
            }
        }
        return output;
    }
}
