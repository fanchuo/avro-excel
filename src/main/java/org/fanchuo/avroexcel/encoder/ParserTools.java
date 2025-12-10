package org.fanchuo.avroexcel.encoder;

import org.apache.avro.Schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class ParserTools {
    public static List<Schema> flatten(Schema schema, Predicate<Schema> predicate) {
        return flatten(Collections.singletonList(schema), predicate);
    }
    public static List<Schema> flatten(List<Schema> schemas, Predicate<Schema> predicate) {
        List<Schema> output = new ArrayList<>();
        collect(schemas, predicate, output);
        return output;
    }

    private static void collect(List<Schema> schemas, Predicate<Schema> predicate, List<Schema> output) {
        for (Schema schema : schemas) {
            if (schema.getType() == Schema.Type.UNION) {
                collect(schema.getTypes(), predicate, output);
            } else if (predicate.test(schema)) {
                output.add(schema);
            }
        }
    }
}
