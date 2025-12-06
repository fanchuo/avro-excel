package org.fanchuo.avroexcel.encoder;

public class ParserResult {
    public static final ParserResult NOT_MATCH = new ParserResult(false, null);
    final boolean compatible;
    final Object payload;

    ParserResult(boolean compatible, Object payload) {
        this.compatible = compatible;
        this.payload = payload;
    }
}
