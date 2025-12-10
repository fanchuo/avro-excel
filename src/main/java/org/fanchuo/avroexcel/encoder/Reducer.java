package org.fanchuo.avroexcel.encoder;

import org.apache.avro.Schema;

public interface Reducer<TSource, TTargetCollection, TIterable> {
    TTargetCollection empty();
    Schema.Type schemaType();
    Schema subSchema(Schema schema);
    Iterable<TIterable> iterable(TSource source);
    ExcelRecord unwrapRecord(TIterable iterable);
    void aggregate(TTargetCollection collection, TIterable item, Object value);
}
