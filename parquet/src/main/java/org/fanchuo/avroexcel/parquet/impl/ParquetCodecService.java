package org.fanchuo.avroexcel.parquet.impl;

import org.fanchuo.avroexcel.core.api.CodecService;
import org.fanchuo.avroexcel.core.avroutil.IGenericDataConf;
import org.fanchuo.avroexcel.core.decoder.IDecoderBuilder;
import org.fanchuo.avroexcel.core.encoder.IEncoderBuilder;
import org.fanchuo.avroexcel.parquet.decoder.ParquetDecoderBuilder;
import org.fanchuo.avroexcel.parquet.encoder.ParquetEncoderBuilder;

public class ParquetCodecService implements CodecService {
  @Override
  public String getName() {
    return "PARQUET";
  }

  @Override
  public IDecoderBuilder makeDecoder(IGenericDataConf genericDataConf) {
    return new ParquetDecoderBuilder(genericDataConf);
  }

  @Override
  public IEncoderBuilder makeEncoder(IGenericDataConf genericDataConf) {
    return new ParquetEncoderBuilder(genericDataConf);
  }
}
